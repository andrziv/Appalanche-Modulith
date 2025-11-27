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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static com.jobhunt.backend.JobHunt_Modulith.SecurityScenarioHelper.*;
import static com.jobhunt.backend.JobHunt_Modulith.SecurityScenarioHelper.SecurityScenario.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class AccountIntegrationTests {
    private final static String USER_FIRST_NAME = "Test";
    private final static String USER_LAST_NAME = "User";
    private final static String USER_EMAIL = "test.user@gmail.com";
    private final static String USER_PASSWORD = "1definitely2Secure";
    private final static String ATTACKER_EMAIL = "other.user@gmail.com";

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

    static List<SecurityScenario> scenarios = Arrays.asList(VALID_USER, EXPIRED_TOKEN, MALFORMED_TOKEN, MODIFIED_TOKEN, FAKE_TOKEN, NO_TOKEN);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldRegisterNewAccountSuccessfully(SecurityScenario scenario) throws Exception {
        var output = registerAccount(USER_EMAIL, USER_PASSWORD, scenario);

        var account = accountRepository.findByEmail(USER_EMAIL);
        output.andExpect(status().isCreated());
        assertThat(account).isPresent();
        assertThat(account.get().getFirstName()).isEqualTo(USER_FIRST_NAME);
        assertThat(account.get().getLastName()).isEqualTo(USER_LAST_NAME);
        assertThat(account.get().getPassword()).isNotEqualTo(USER_PASSWORD);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailRegistrationWithDuplicateEmailInput(SecurityScenario scenario) throws Exception {
        registerAccount(USER_EMAIL, USER_PASSWORD, scenario);
        var output = registerAccount(USER_EMAIL, USER_PASSWORD, scenario);
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
        var invalidEmail = "testuser";

        var output = registerAccount(invalidEmail, USER_PASSWORD, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(status().isBadRequest());
        assertThat(accountRepository.findByEmail(invalidEmail)).isEmpty();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getErrorMessage()).isEqualTo(null);
        assertEquals("{\"email\":\"Invalid email format\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailRegistrationWithInvalidPasswordInput(SecurityScenario scenario) throws Exception {
        var invalidPassword = "pass1";

        var output = registerAccount(USER_EMAIL, invalidPassword, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(status().isBadRequest());
        assertThat(accountRepository.findByEmail(USER_EMAIL)).isEmpty();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getErrorMessage()).isEqualTo(null);
        assertEquals("{\"password\":\"Password must be between 8 and 30 characters\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldLoginSuccessfullyWithValidInputs(SecurityScenario scenario) throws Exception {
        registerAccount(USER_EMAIL, USER_PASSWORD, scenario);
        var output = authenticateAccount(USER_PASSWORD, scenario);
        var response = output.andReturn().getResponse();

        String token = JsonPath.read(response.getContentAsString(), "$.token");
        output.andExpect(status().isOk());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(jwtHelper.extractUsername(token)).isEqualTo(USER_EMAIL);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldRejectLoginWithWrongPassword(SecurityScenario scenario) throws Exception {
        registerAccount(USER_EMAIL, USER_PASSWORD, scenario);
        var output = authenticateAccount(USER_PASSWORD + '1', scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(status().isUnauthorized());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isEqualTo(null);
        assertEquals("{\"error\":\"Login Failed\",\"message\":\"Invalid email or password\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @NotNull
    private ResultActions registerAccount(String email, String password, SecurityScenario scenario) throws Exception {
        SignupRequest signupData = new SignupRequest(USER_FIRST_NAME, USER_LAST_NAME, email, password);

        var userAccount = new Account(USER_FIRST_NAME, USER_LAST_NAME, email, password);
        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = post("/authenticate/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupData));

        if (headerName != null) {
            request.header(headerName, headerValue);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions authenticateAccount(String password, SecurityScenario scenario) throws Exception {
        LoginRequest requestData = new LoginRequest(USER_EMAIL, password);

        var userAccount = new Account(USER_FIRST_NAME, USER_LAST_NAME, USER_EMAIL, password);
        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);


        var request = post("/authenticate/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (headerName != null) {
            request.header(headerName, headerValue);
        }

        return mockMvc.perform(request);
    }
}
