package com.jobhunt.backend.JobHunt_Modulith.authentication.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jobhunt.backend.JobHunt_Modulith.authentication.business.request_response.LoginRequest;
import com.jobhunt.backend.JobHunt_Modulith.authentication.business.request_response.SignupRequest;
import com.jobhunt.backend.JobHunt_Modulith.authentication.persistence.Account;
import com.jobhunt.backend.JobHunt_Modulith.authentication.persistence.AccountRepository;
import com.jobhunt.backend.JobHunt_Modulith.security.helper.JwtHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static com.jobhunt.backend.JobHunt_Modulith.authentication.endpoint.AccountIntegrationTests.SecurityScenario.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class AccountIntegrationTests {
    enum SecurityScenario {
        VALID_USER, WRONG_USER, EXPIRED_TOKEN, MALFORMED_TOKEN, NO_TOKEN
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected JwtHelper jwtHelper;

    @Autowired
    private AccountRepository accountRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private static final Account otherUser = new Account("Other", "User", "not.main@gmail.com", "blarrrgh@123");

    static List<SecurityScenario> scenarios = Arrays.asList(VALID_USER, WRONG_USER, EXPIRED_TOKEN, MALFORMED_TOKEN, NO_TOKEN);

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldRegisterNewAccountSuccessfully(SecurityScenario scenario) throws Exception {
        var firstname = "Test";
        var lastname = "User";
        var email = "test.user@gmail.com";
        var password = "1definitely2Secure";

        var output = registerAccount(firstname, lastname, email, password, scenario);

        var account = accountRepository.findByEmail(email);
        output.andExpect(status().isCreated());
        assertThat(account).isPresent();
        assertThat(account.get().getFirstName()).isEqualTo("Test");
        assertThat(account.get().getLastName()).isEqualTo("User");
        assertThat(account.get().getPassword()).isNotEqualTo("1definitely2Secure");
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailRegistrationWithDuplicateEmailInput(SecurityScenario scenario) throws Exception {
        var firstname = "Test";
        var lastname = "User";
        var email = "test.user@gmail.com";
        var password = "1definitely2Secure";

        registerAccount(firstname, lastname, email, password, scenario);
        var output = registerAccount(lastname, firstname, email, password, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(status().isConflict());
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getErrorMessage()).isEqualTo(null);
        assertEquals("{\"error\":\"Conflict\",\"message\":\"User with the email address 'test.user@gmail.com' already exists\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailRegistrationWithInvalidEmailInput(SecurityScenario scenario) throws Exception {
        var firstname = "Test";
        var lastname = "User";
        var email = "testuser";
        var password = "1definitely2Secure";

        var output = registerAccount(firstname, lastname, email, password, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(status().isBadRequest());
        assertThat(accountRepository.findByEmail(email)).isEmpty();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getErrorMessage()).isEqualTo(null);
        assertEquals("{\"email\":\"Invalid email format\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailRegistrationWithInvalidPasswordInput(SecurityScenario scenario) throws Exception {
        var firstname = "Test";
        var lastname = "User";
        var email = "test.user@gmail.com";
        var password = "pass1";

        var output = registerAccount(firstname, lastname, email, password, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(status().isBadRequest());
        assertThat(accountRepository.findByEmail(email)).isEmpty();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getErrorMessage()).isEqualTo(null);
        assertEquals("{\"password\":\"Password must be between 8 and 30 characters\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldLoginSuccessfullyWithValidInputs(SecurityScenario scenario) throws Exception {
        var firstname = "Test";
        var lastname = "User";
        var email = "test.user@gmail.com";
        var password = "1definitely2Secure";

        registerAccount(firstname, lastname, email, password, scenario);
        var output = authenticateAccount(email, password, scenario);
        var response = output.andReturn().getResponse();

        String token = JsonPath.read(response.getContentAsString(), "$.token");
        output.andExpect(status().isOk());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(jwtHelper.extractUsername(token)).isEqualTo(email);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldRejectLoginWithWrongPassword(SecurityScenario scenario) throws Exception {
        var firstname = "Test";
        var lastname = "User";
        var email = "test.user@gmail.com";
        var password = "1definitely2Secure";

        registerAccount(firstname, lastname, email, password, scenario);
        var output = authenticateAccount(email, password + '1', scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(status().isUnauthorized());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo(null);
        assertEquals("{\"error\":\"Login Failed\",\"message\":\"Invalid email or password\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @NotNull
    private ResultActions registerAccount(String firstname, String lastname, String email, String password, SecurityScenario scenario) throws Exception {
        SignupRequest request = new SignupRequest(firstname, lastname, email, password);

        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, new Account("", "", email, password));

        if (headerName != null) {
            return mockMvc.perform(post("/authenticate/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(headerName, headerValue)
                    .content(objectMapper.writeValueAsString(request)));

        }

        return mockMvc.perform(post("/authenticate/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @NotNull
    private ResultActions authenticateAccount(String email, String password, SecurityScenario scenario) throws Exception {
        LoginRequest request = new LoginRequest(email, password);

        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, new Account("", "", email, password));

        if (headerName != null) {
            return mockMvc.perform(post("/authenticate/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(headerName, headerValue)
                    .content(objectMapper.writeValueAsString(request)));
        }

        return mockMvc.perform(post("/authenticate/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private String generateHeaderNameForScenario(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER,
                 WRONG_USER,
                 MALFORMED_TOKEN,
                 EXPIRED_TOKEN -> AUTHORIZATION;
            case NO_TOKEN -> null;
        };
    }

    private String generateHeaderValueForScenario(SecurityScenario scenario, Account owner) {
        return switch (scenario) {
            case VALID_USER -> "Bearer " + jwtHelper.generateToken(owner);
            case WRONG_USER -> "Bearer " + jwtHelper.generateToken(otherUser);
            case MALFORMED_TOKEN -> "Bearer not.a.valid.token";
            case EXPIRED_TOKEN -> {
                var oldHelper = new JwtHelper(secretKey, -1000);
                yield "Bearer " + oldHelper.generateToken(owner);
            }
            case NO_TOKEN -> null;
        };
    }
}
