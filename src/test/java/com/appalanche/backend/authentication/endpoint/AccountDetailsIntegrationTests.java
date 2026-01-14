package com.appalanche.backend.authentication.endpoint;

import com.appalanche.backend.SecurityScenarioHelper;
import com.appalanche.backend.authentication.business.request_response.ChangeEmailRequest;
import com.appalanche.backend.authentication.business.request_response.ChangePasswordRequest;
import com.appalanche.backend.authentication.persistence.AccountRepository;
import com.appalanche.backend.authentication.persistence.RefreshTokenRepository;
import com.appalanche.backend.authentication.persistence.dao.Account;
import com.appalanche.backend.authentication.persistence.dao.RefreshToken;
import com.appalanche.backend.security.helper.JwtHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.*;

import static com.appalanche.backend.SecurityScenarioHelper.SecurityScenario;
import static com.appalanche.backend.SecurityScenarioHelper.SecurityScenario.*;
import static com.appalanche.backend.SecurityScenarioHelper.generateCookieForScenario;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class AccountDetailsIntegrationTests {
    private final static String USER_AGENT_HEADER = "Test-Runner 9.99.9";
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

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    static List<SecurityScenarioHelper.SecurityScenario> scenarios = Arrays.asList(VALID_USER, EXPIRED_TOKEN, MALFORMED_TOKEN, MODIFIED_TOKEN, FAKE_TOKEN, NO_TOKEN);

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
    public void shouldSuccessfullyUpdatePasswordAndClearOldTokens(SecurityScenario scenario) throws Exception {
        var account = accountRepository.save(new Account(randomUUID(), USER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var expect1 = tokenRepository.save(createRefreshToken(account, "DELETE1_IFVALID", now()));
        var expect2 = tokenRepository.save(createRefreshToken(account, "DELETE2_IFVALID", now()));
        var expect3 = tokenRepository.save(createRefreshToken(account, "DELETE3_IFVALID", now()));
        var expect4 = tokenRepository.save(createRefreshToken(account, "DELETE4_IFVALID", now()));

        var output = updatePassword(account, "validNewPassword123", scenario);
        var response = output.andReturn().getResponse();

        var refreshCookie = getRefreshToken(response);
        var expectedValidList = refreshCookie == null ? new ArrayList<RefreshToken>() : List.of(refreshCookie);
        output.andExpect(expectedHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectedStatusCodeFor(scenario));
        expectJwtToken(response, true, false, scenario);
        expectRefreshTokens(account.getAccountId(), expectedValidList, List.of(expect1, expect2, expect3, expect4), scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    public void shouldSuccessfullyUpdateEmailAndClearOldTokens(SecurityScenario scenario) throws Exception {
        var account = accountRepository.save(new Account(randomUUID(), USER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var expect1 = tokenRepository.save(createRefreshToken(account, "DELETE1_IFVALID", now()));
        var expect2 = tokenRepository.save(createRefreshToken(account, "DELETE2_IFVALID", now()));
        var expect3 = tokenRepository.save(createRefreshToken(account, "DELETE3_IFVALID", now()));
        var expect4 = tokenRepository.save(createRefreshToken(account, "DELETE4_IFVALID", now()));

        var output = updateEmail(account, USER_PASSWORD, "totallygood@email.com", scenario);
        var response = output.andReturn().getResponse();

        var refreshCookie = getRefreshToken(response);
        var expectedValidList = refreshCookie == null ? new ArrayList<RefreshToken>() : List.of(refreshCookie);
        output.andExpect(expectedHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectedStatusCodeFor(scenario));
        expectJwtToken(response, true, false, scenario);
        expectRefreshTokens(account.getAccountId(), expectedValidList, List.of(expect1, expect2, expect3, expect4), scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    public void shouldFailUpdatingPasswordIfNewPasswordIsInvalid(SecurityScenario scenario) throws Exception {
        var account = accountRepository.save(new Account(randomUUID(), USER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var expect1 = tokenRepository.save(createRefreshToken(account, "KEEP1", now()));
        var expect2 = tokenRepository.save(createRefreshToken(account, "KEEP2", now()));
        var expect3 = tokenRepository.save(createRefreshToken(account, "KEEP3", now()));
        var expect4 = tokenRepository.save(createRefreshToken(account, "KEEP4", now()));

        var output = updatePassword(account, "pass", scenario);
        var response = output.andReturn().getResponse();

        var expectedRefreshList = List.of(expect1, expect2, expect3, expect4);
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        expectJwtToken(response, false, false, scenario);
        expectRefreshTokens(account.getAccountId(), expectedRefreshList, expectedRefreshList, scenario);
        assertFailedValidationContent(response, "{\"newPassword\":\"Password must be between 8 and 30 characters\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    public void shouldFailUpdatingPasswordIfOldPasswordIsIncorrect(SecurityScenario scenario) throws Exception {
        var account = accountRepository.save(new Account(randomUUID(), USER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var expect1 = tokenRepository.save(createRefreshToken(account, "KEEP1", now()));
        var expect2 = tokenRepository.save(createRefreshToken(account, "KEEP2", now()));
        var expect3 = tokenRepository.save(createRefreshToken(account, "KEEP3", now()));
        var expect4 = tokenRepository.save(createRefreshToken(account, "KEEP4", now()));

        var output = updatePassword(account, "wrongOldPassword", "validNewPassword123", scenario);
        var response = output.andReturn().getResponse();

        var expectedRefreshList = List.of(expect1, expect2, expect3, expect4);
        output.andExpect(status().isUnauthorized());
        assertThat(response.getStatus()).isEqualTo(401);
        expectJwtToken(response, false, false, scenario);
        expectRefreshTokens(account.getAccountId(), expectedRefreshList, expectedRefreshList, scenario);
        assertFailedValidationContent(response, "{\"message\":\"Invalid email or password\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    public void shouldFailUpdatingEmailIfNewEmailIsInvalid(SecurityScenario scenario) throws Exception {
        var account = accountRepository.save(new Account(randomUUID(), USER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var expect1 = tokenRepository.save(createRefreshToken(account, "KEEP1", now()));
        var expect2 = tokenRepository.save(createRefreshToken(account, "KEEP2", now()));
        var expect3 = tokenRepository.save(createRefreshToken(account, "KEEP3", now()));
        var expect4 = tokenRepository.save(createRefreshToken(account, "KEEP4", now()));

        var output = updateEmail(account, USER_PASSWORD, "nobueno", scenario);
        var response = output.andReturn().getResponse();

        var expectedRefreshList = List.of(expect1, expect2, expect3, expect4);
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        expectJwtToken(response, false, false, scenario);
        expectRefreshTokens(account.getAccountId(), expectedRefreshList, expectedRefreshList, scenario);
        assertFailedValidationContent(response, "{\"newEmail\":\"Invalid email format\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    public void shouldFailUpdatingEmailIfNewEmailIsUsedElsewhere(SecurityScenario scenario) throws Exception {
        var account = accountRepository.save(new Account(randomUUID(), USER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var otherAccount = accountRepository.save(new Account(randomUUID(), ATTACKER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var expect1 = tokenRepository.save(createRefreshToken(account, "KEEP1", now()));
        var expect2 = tokenRepository.save(createRefreshToken(account, "KEEP2", now()));
        var expect3 = tokenRepository.save(createRefreshToken(otherAccount, "KEEP3", now()));
        var expect4 = tokenRepository.save(createRefreshToken(otherAccount, "KEEP4", now()));

        var output = updateEmail(otherAccount, USER_PASSWORD, USER_EMAIL, scenario);
        var response = output.andReturn().getResponse();

        var expectedRefreshList1 = List.of(expect1, expect2);
        var expectedRefreshList2 = List.of(expect3, expect4);
        output.andExpect(conflictHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectConflictStatusCode(scenario));
        expectJwtToken(response, false, false, scenario);
        expectRefreshTokens(account.getAccountId(), expectedRefreshList1, expectedRefreshList1, scenario);
        expectRefreshTokens(otherAccount.getAccountId(), expectedRefreshList2, expectedRefreshList2, scenario);
        assertFailedValidationContent(response, "{\"message\":\"User with the email address 'test.user@gmail.com' already exists\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    public void shouldFailUpdatingEmailIfPasswordIsIncorrect(SecurityScenario scenario) throws Exception {
        var account = accountRepository.save(new Account(randomUUID(), USER_EMAIL, passwordEncoder.encode(USER_PASSWORD)));
        var expect1 = tokenRepository.save(createRefreshToken(account, "KEEP1", now()));
        var expect2 = tokenRepository.save(createRefreshToken(account, "KEEP2", now()));
        var expect3 = tokenRepository.save(createRefreshToken(account, "KEEP3", now()));
        var expect4 = tokenRepository.save(createRefreshToken(account, "KEEP4", now()));

        var output = updateEmail(account, "wrongOldPassword", "totallygood@email.com", scenario);
        var response = output.andReturn().getResponse();

        var expectedRefreshList = List.of(expect1, expect2, expect3, expect4);
        output.andExpect(status().isUnauthorized());
        assertThat(response.getStatus()).isEqualTo(401);
        expectJwtToken(response, false, false, scenario);
        expectRefreshTokens(account.getAccountId(), expectedRefreshList, expectedRefreshList, scenario);
        assertFailedValidationContent(response, "{\"message\":\"Invalid email or password\"}", scenario);
    }

    @NotNull
    private ResultActions updatePassword(Account account, String newPassword, SecurityScenario scenario) throws Exception {
        var requestData = new ChangePasswordRequest(USER_PASSWORD, newPassword);

        var cookie = generateCookieForScenario(scenario, account, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = patch("/account/change-password")
                .header("User-Agent", USER_AGENT_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions updatePassword(Account account, String oldPassword, String newPassword, SecurityScenario scenario) throws Exception {
        var requestData = new ChangePasswordRequest(oldPassword, newPassword);

        var cookie = generateCookieForScenario(scenario, account, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = patch("/account/change-password")
                .header("User-Agent", USER_AGENT_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions updateEmail(Account account, String password, String newEmail, SecurityScenario scenario) throws Exception {
        var requestData = new ChangeEmailRequest(password, newEmail);

        var cookie = generateCookieForScenario(scenario, account, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = patch("/account/change-email")
                .header("User-Agent", USER_AGENT_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    private void expectJwtToken(MockHttpServletResponse response, boolean expectWithValidInput, boolean expectWithInvalidInput, SecurityScenario scenario) {
        switch (scenario) {
            case VALID_USER -> {
                var jwtCookie = response.getCookie("accessToken");
                if (expectWithValidInput) {
                    assertThat(jwtCookie).isNotNull();
                    assertThat(jwtCookie.getValue()).isNotBlank();
                } else {
                    assertThat(jwtCookie).isNull();
                }
            }
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> {
                var jwtCookie = response.getCookie("accessToken");
                if (expectWithInvalidInput) {
                    assertThat(jwtCookie).isNotNull();
                    assertThat(jwtCookie.getValue()).isNotBlank();
                } else {
                    assertThat(jwtCookie).isNull();
                }
            }
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    private void expectRefreshTokenCount(UUID accountId, long happyPathExpectation, long sadPathExpectation, SecurityScenario scenario) {
        switch (scenario) {
            case VALID_USER -> assertThat(getRefreshTokenCountFor(accountId)).isEqualTo(happyPathExpectation);
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertThat(getRefreshTokenCountFor(accountId)).isEqualTo(sadPathExpectation);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    private void expectRefreshTokens(UUID accountId, List<RefreshToken> happyPathExpectation, List<RefreshToken> sadPathExpectation, SecurityScenario scenario) {
        var actualTokens = tokenRepository.findAllByAccount_AccountIdOrderByLastUsedAsc(accountId);
        switch (scenario) {
            case VALID_USER -> assertRefreshTokenListsEqual(happyPathExpectation, actualTokens);
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertRefreshTokenListsEqual(sadPathExpectation, actualTokens);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    private void assertRefreshTokenListsEqual(List<RefreshToken> expected, List<RefreshToken> actual) {
        assertThat(actual).usingRecursiveComparison()
                          .withEqualsForType((instant1, instant2) -> {
                                      if (instant1 == null || instant2 == null) {
                                          return false;
                                      }
                                      return instant1.truncatedTo(SECONDS).equals(instant2.truncatedTo(SECONDS));
                                  }, Instant.class
                          )
                          .withEqualsForType((account1, account2) -> {
                                      if (account1 == null || account2 == null) {
                                          return false;
                                      }
                                      return account1.getId().equals(account2.getId());
                                  }, Account.class
                          )
                          .ignoringCollectionOrder()
                          .isEqualTo(expected);
    }

    private void assertFailedValidationContent(
            MockHttpServletResponse response, String expectedContent, SecurityScenario scenario) throws Exception {
        switch (scenario) {
            case VALID_USER -> assertThat(response.getContentAsString()).isEqualTo(expectedContent);
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

    private static ResultMatcher badRequestHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isBadRequest();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> expectedHttpStatusMatcherFor(scenario);
        };
    }

    private static ResultMatcher conflictHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isConflict();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> expectedHttpStatusMatcherFor(scenario);
        };
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

    private static int expectBadRequestStatusCode(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 400;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> expectedStatusCodeFor(scenario);
        };
    }

    private static int expectConflictStatusCode(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 409;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> expectedStatusCodeFor(scenario);
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

    private RefreshToken createRefreshToken(Account account, String deviceName, Instant lastUsed) {
        return new RefreshToken(account, randomUUID(), randomUUID().toString(), deviceName, now().plus(10, SECONDS), lastUsed);
    }

    private RefreshToken getRefreshToken(MockHttpServletResponse response) {
        var cookie = response.getCookie("refreshToken");
        if (cookie == null) {
            return null;
        }

        if (cookie.getValue() == null) {
            return null;
        }

        return tokenRepository.findByToken(cookie.getValue()).orElse(null);
    }

    private long getRefreshTokenCountFor(UUID ownerAccountId) {
        return tokenRepository.findAllByAccount_AccountIdOrderByLastUsedAsc(ownerAccountId).size();
    }
}
