package com.appalanche.backend.authentication.endpoint;

import com.appalanche.backend.authentication.business.request_response.LoginRequest;
import com.appalanche.backend.authentication.business.request_response.SignupRequest;
import com.appalanche.backend.authentication.persistence.AccountRepository;
import com.appalanche.backend.authentication.persistence.RefreshTokenRepository;
import com.appalanche.backend.authentication.persistence.dao.Account;
import com.appalanche.backend.security.helper.JwtHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.appalanche.backend.SecurityScenarioHelper.SecurityScenario;
import static com.appalanche.backend.SecurityScenarioHelper.SecurityScenario.*;
import static com.appalanche.backend.SecurityScenarioHelper.generateCookieForScenario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class AccountIntegrationTests {
    private final static UUID USER_ACCOUNT_ID = UUID.randomUUID();
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

    @Autowired
    private RefreshTokenRepository tokenRepository;

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
        tokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldRegisterNewAccountSuccessfully(SecurityScenario scenario) throws Exception {
        var output = registerAccount(USER_EMAIL, USER_PASSWORD, scenario);

        var account = accountRepository.findByEmail(USER_EMAIL);
        output.andExpect(status().isCreated());
        assertThat(account).isPresent();
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
        assertEquals("{\"message\":\"User with the email address 'test.user@gmail.com' already exists\"}",
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

        var jwtCookie = response.getCookie("accessToken");
        output.andExpect(status().isOk());
        assertNotNull(jwtCookie);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(jwtHelper.extractAccountId(jwtCookie.getValue())).isNotNull();
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
        assertEquals("{\"message\":\"Invalid email or password\"}",
                response.getContentAsString(),
                NON_EXTENSIBLE);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldAuthenticateSuccessfullyWithValidAccessToken(SecurityScenario scenario) throws Exception {
        registerAccount(USER_EMAIL, USER_PASSWORD, scenario);
        var createdAccount = accountRepository.findByEmail(USER_EMAIL);
        var accountId = createdAccount.get().getAccountId();

        var output = authenticateToken(accountId, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(expectedHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectedStatusCodeFor(scenario));
        assertAuthenticationResponse(response, scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldLogoutSuccessfullyWithValidAccessTokenButNoRefreshToken(SecurityScenario scenario) throws Exception {
        registerAccount(USER_EMAIL, USER_PASSWORD, scenario);
        var createdAccount = accountRepository.findByEmail(USER_EMAIL);
        var accountId = createdAccount.get().getAccountId();

        var output = logout(accountId, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(expectedHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectedStatusCodeFor(scenario));
        assertLogoutResponse(response, scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldLogoutSuccessfullyWithValidAccessTokenAndRefreshToken(SecurityScenario scenario) throws Exception {
        registerAccount(USER_EMAIL, USER_PASSWORD, scenario);
        var createdAccount = accountRepository.findByEmail(USER_EMAIL);
        var accountId = createdAccount.get().getAccountId();

        var output = logout(accountId, scenario);
        var response = output.andReturn().getResponse();

        output.andExpect(expectedHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectedStatusCodeFor(scenario));
        assertLogoutResponse(response, scenario);
    }

    @NotNull
    private ResultActions registerAccount(String email, String password, SecurityScenario scenario) throws Exception {
        SignupRequest signupData = new SignupRequest(USER_FIRST_NAME, USER_LAST_NAME, email, password);

        var userAccount = new Account(USER_ACCOUNT_ID, email, password);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = post("/authenticate/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions authenticateAccount(String password, SecurityScenario scenario) throws Exception {
        LoginRequest requestData = new LoginRequest(USER_EMAIL, password);

        var userAccount = new Account(USER_ACCOUNT_ID, USER_EMAIL, password);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = post("/authenticate/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions authenticateToken(UUID accountId, SecurityScenario scenario) throws Exception {
        var userAccount = new Account(accountId, USER_EMAIL, USER_PASSWORD);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = get("/authenticate").contentType(MediaType.APPLICATION_JSON);

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions logout(UUID accountId, SecurityScenario scenario) throws Exception {
        var userAccount = new Account(accountId, USER_EMAIL, USER_PASSWORD);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = post("/authenticate/logout").contentType(MediaType.APPLICATION_JSON);

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    private static ResultMatcher expectedHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isOk();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> status().isUnauthorized();
        };
    }

    private static int expectedStatusCodeFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 200;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> 401;
        };
    }

    private void assertAuthenticationResponse(MockHttpServletResponse response, SecurityScenario scenario) throws Exception {
        switch (scenario) {
            case VALID_USER -> {
                String email = JsonPath.read(response.getContentAsString(), "$.email");
                UUID accountId = UUID.fromString(JsonPath.read(response.getContentAsString(), "$.accountId"));
                var existingAccount = accountRepository.findByEmail(USER_EMAIL);
                assertThat(existingAccount).isPresent();
                assertThat(email).isEqualTo(USER_EMAIL);
                assertThat(accountId).isEqualTo(existingAccount.get().getAccountId());
            }
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertGenericEndpointResponse(response, scenario);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    private void assertLogoutResponse(MockHttpServletResponse response, SecurityScenario scenario) throws Exception {
        switch (scenario) {
            case VALID_USER -> {
                var cookie = response.getCookie("accessToken");
                assertThat(cookie).isNotNull();
                assertThat(cookie.getMaxAge()).isEqualTo(0);
            }
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertGenericEndpointResponse(response, scenario);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    private void assertGenericEndpointResponse(MockHttpServletResponse response, SecurityScenario scenario) throws UnsupportedEncodingException, JsonProcessingException {
        switch (scenario) {
            case VALID_USER -> fail("Implement the correct assertion!");
            case MALFORMED_TOKEN -> assertThat(response.getContentAsString())
                    .isEqualTo("""
                            {
                              "error": "Invalid Token",
                              "details": "Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 3"
                            }
                            """);
            case EXPIRED_TOKEN -> {
                Map<String, Object> jsonBody = objectMapper.readValue(
                        response.getContentAsString(),
                        new TypeReference<>() {
                        }
                );

                assertThat(jsonBody)
                        .containsEntry("error", "Token has expired");

                assertThat((String) jsonBody.get("details"))
                        .startsWith("JWT expired")
                        .contains("milliseconds ago at")
                        .contains("Current time:");
            }
            case MODIFIED_TOKEN,
                 FAKE_TOKEN -> assertThat(response.getContentAsString())
                    .isEqualTo("""
                            {
                              "error": "Invalid Token",
                              "details": "JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted."
                            }
                            """);
            case NO_TOKEN -> assertThat(response.getContentAsString())
                    .isEqualTo("""
                            {
                              "error": "Unauthorized",
                              "details": "Authentication required"
                            }
                            """);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }
}
