package com.appalanche.backend.profiles;

import com.appalanche.backend.authentication.business.events.AccountCreationEvent;
import com.appalanche.backend.authentication.business.request_response.SignupRequest;
import com.appalanche.backend.authentication.persistence.AccountRepository;
import com.appalanche.backend.authentication.persistence.dao.Account;
import com.appalanche.backend.profiles.business.request_response.AddJobSiteRequest;
import com.appalanche.backend.profiles.business.request_response.GetAccountProfileResponse;
import com.appalanche.backend.profiles.business.request_response.ModifyAccountProfileRequest;
import com.appalanche.backend.profiles.persistence.AccountProfileRepository;
import com.appalanche.backend.profiles.persistence.JobSiteRepository;
import com.appalanche.backend.profiles.persistence.dao.AccountProfile;
import com.appalanche.backend.profiles.persistence.dao.JobSite;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static com.appalanche.backend.SecurityScenarioHelper.SecurityScenario;
import static com.appalanche.backend.SecurityScenarioHelper.SecurityScenario.*;
import static com.appalanche.backend.SecurityScenarioHelper.generateCookieForScenario;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RecordApplicationEvents
@Testcontainers
public class AccountProfileIntegrationTests {
    private final static UUID USER_ACCOUNT_ID = randomUUID();
    private final static String USER_FIRST_NAME = "Test";
    private final static String USER_LAST_NAME = "User";
    private final static String USER_EMAIL = "test.user@gmail.com";
    private final static String USER_PASSWORD = "1definitely2Secure";
    private final static String ATTACKER_EMAIL = "attacker.user@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected JwtHelper jwtHelper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountProfileRepository accountProfileRepository;

    @Autowired
    private JobSiteRepository jobSiteRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

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
        accountProfileRepository.deleteAll();
        jobSiteRepository.deleteAll();
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldAutomaticallyRegisterNewAccountProfileSuccessfully(SecurityScenario scenario) throws Exception {
        var output = registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        UUID accountId = accountRepository.findByEmail(USER_EMAIL).get().getAccountId();

        output.andExpect(status().isCreated());
        var account = accountProfileRepository.findByAccountId(accountId);
        assertThat(account).isPresent();
        assertThat(account.get().getFirstName()).isEqualTo(USER_FIRST_NAME);
        assertThat(account.get().getLastName()).isEqualTo(USER_LAST_NAME);
        applicationEvents.stream(AccountCreationEvent.class)
                         .filter(event -> event.accountId().equals(accountId))
                         .findFirst()
                         .ifPresent(event -> {
                             assertThat(event.firstName()).isEqualTo(USER_FIRST_NAME);
                             assertThat(event.lastName()).isEqualTo(USER_LAST_NAME);
                         });
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldSuccessfullyModifyAccountProfileWithValidAccessToken(SecurityScenario scenario) throws Exception {
        registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        UUID accountId = accountRepository.findByEmail(USER_EMAIL).get().getAccountId();
        var newFirstName = "New First";
        var newLastName = "New Last";
        var newLinkedIn = "https://linkedin.com/blah";
        var newGitHub = "https://github.com/blah";
        var newPortfolio = "https://test.blah.dev";

        var output = modifyProfile(accountId, newFirstName, newLastName, newLinkedIn, newGitHub, newPortfolio, scenario);

        var expectedAccount = new AccountProfile(accountId, newFirstName, newLastName, newLinkedIn, newGitHub, newPortfolio);
        output.andExpect(noContentHttpStatusMatcherFor(scenario));
        assertAccountContentsGivenScenario(expectedAccount, List.of(), output.andReturn().getResponse(), scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldSuccessfullyNullAccountProfileURLsUsingBlankInputs(SecurityScenario scenario) throws Exception {
        registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        UUID accountId = accountRepository.findByEmail(USER_EMAIL).get().getAccountId();
        directlyModifyProfile(accountId, "https://linkedin.com/blah", "https://github.com/blah", "https://test.blah.dev");
        var newLinkedIn = " ";
        var newGitHub = " ";
        var newPortfolio = " ";

        var output = modifyProfile(accountId, null, null, newLinkedIn, newGitHub, newPortfolio, scenario);

        var expectedAccount = new AccountProfile(accountId, USER_FIRST_NAME, USER_LAST_NAME, null, null, null);
        output.andExpect(noContentHttpStatusMatcherFor(scenario));
        assertAccountContentsGivenScenario(expectedAccount, List.of(), output.andReturn().getResponse(), scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailModifyingAccountProfileOfOtherUser(SecurityScenario scenario) throws Exception {
        var otherUserEmail = "other.user@gmail.com";
        registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        var attackerAccount = accountRepository.findByEmail(USER_EMAIL).get();
        registerAccountAndWait(otherUserEmail, USER_PASSWORD, scenario);
        UUID otherAccountId = accountRepository.findByEmail(otherUserEmail).get().getAccountId();
        var newFirstName = "New First";
        var newLastName = "New Last";
        var newLinkedIn = "https://linkedin.com/blah";
        var newGitHub = "https://github.com/blah";
        var newPortfolio = "https://test.blah.dev";

        var output = modifyProfile(attackerAccount, newFirstName, newLastName, newLinkedIn, newGitHub, newPortfolio, scenario);

        var expectedAccount = new AccountProfile(otherAccountId, USER_FIRST_NAME, USER_LAST_NAME, null, null, null);
        output.andExpect(noContentHttpStatusMatcherFor(scenario));
        assertAccountContentsGivenScenario(expectedAccount, List.of(), output.andReturn().getResponse(), scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailModifyingAccountProfileWithBlankFirstNameField(SecurityScenario scenario) throws Exception {
        registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        UUID accountId = accountRepository.findByEmail(USER_EMAIL).get().getAccountId();
        var newFirstName = " ";
        var newLastName = "New Last";
        var newLinkedIn = "https://linkedin.com/blah";
        var newGitHub = "https://github.com/blah";
        var newPortfolio = "https://test.blah.dev";

        var output = modifyProfile(accountId, newFirstName, newLastName, newLinkedIn, newGitHub, newPortfolio, scenario);

        var response = output.andReturn().getResponse();
        var expectedAccount = new AccountProfile(accountId, USER_FIRST_NAME, USER_LAST_NAME, null, null, null);
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertAccountContentsGivenScenario(expectedAccount, List.of(), response, scenario);
        assertFailedValidationContent(response, "{\"firstname\":\"First name cannot be blank if provided\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailModifyingAccountProfileWithBlankLastNameField(SecurityScenario scenario) throws Exception {
        registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        UUID accountId = accountRepository.findByEmail(USER_EMAIL).get().getAccountId();
        var newFirstName = "New First";
        var newLastName = " ";
        var newLinkedIn = "https://linkedin.com/blah";
        var newGitHub = "https://github.com/blah";
        var newPortfolio = "https://test.blah.dev";

        var output = modifyProfile(accountId, newFirstName, newLastName, newLinkedIn, newGitHub, newPortfolio, scenario);

        var response = output.andReturn().getResponse();
        var expectedAccount = new AccountProfile(accountId, USER_FIRST_NAME, USER_LAST_NAME, null, null, null);
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertAccountContentsGivenScenario(expectedAccount, List.of(), response, scenario);
        assertFailedValidationContent(response, "{\"surname\":\"Last name cannot be blank if provided\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldAddJobSiteToAccountProfile(SecurityScenario scenario) throws Exception {
        registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        UUID accountId = accountRepository.findByEmail(USER_EMAIL).get().getAccountId();
        String jobSite = "https://test.com";

        var output = addJobSite(accountId, jobSite, scenario);
        var resultantProfile = getProfileViaUri(accountId, scenario);

        var response = output.andReturn().getResponse();
        var expectedAccount = resultantProfile.orElse(null);
        output.andExpect(createdHttpStatusMatcherFor(scenario));
        assertAccountContentsGivenScenario(expectedAccount, List.of(jobSite), response, scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldRemoveJobSiteToAccountProfile(SecurityScenario scenario) throws Exception {
        registerAccountAndWait(USER_EMAIL, USER_PASSWORD, scenario);
        registerAccountAndWait(ATTACKER_EMAIL, USER_PASSWORD, scenario);
        var accountId = accountRepository.findByEmail(USER_EMAIL).get().getAccountId();
        var accountId2 = accountRepository.findByEmail(ATTACKER_EMAIL).get().getAccountId();
        String jobSite = "https://test.com";
        var jobSiteId = randomUUID();
        jobSiteRepository.save(new JobSite(jobSiteId, jobSite, "Test"));
        addJobSite(accountId, jobSite, scenario);
        addJobSite(accountId2, jobSite, scenario);

        var output = removeJobSite(accountId, jobSiteId, scenario);
        var resultantProfile = getProfileViaUri(accountId, scenario);

        var response = output.andReturn().getResponse();
        var expectedAccount = resultantProfile.orElse(null);
        output.andExpect(noContentHttpStatusMatcherFor(scenario));
        assertAccountContentsGivenScenario(expectedAccount, List.of(), response, scenario);
        assertThat(jobSiteRepository.findByUrl(jobSite)).isPresent();
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    private ResultActions registerAccountAndWait(String email, String password, SecurityScenario scenario) throws Exception {
        SignupRequest signupData = new SignupRequest(USER_FIRST_NAME, USER_LAST_NAME, email, password);

        var userAccount = new Account(USER_ACCOUNT_ID, email, password);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = post("/authenticate/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        var resultAction = mockMvc.perform(request);
        awaitProfileCreation(accountRepository.findByEmail(email).get().getAccountId());
        return resultAction;
    }

    private void awaitProfileCreation(UUID accountId) {
        await().atMost(5, SECONDS).until(() -> accountProfileRepository.findByAccountId(accountId).isPresent());
    }

    @NotNull
    private ResultActions modifyProfile(UUID accountId, String firstName, String lastName, String linkedIn, String github,
                                        String portfolio, SecurityScenario scenario) throws Exception {
        var requestData = new ModifyAccountProfileRequest(firstName, lastName, linkedIn, github, portfolio);

        var userAccount = new Account(accountId, USER_EMAIL, USER_PASSWORD);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = patch("/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions modifyProfile(Account attacker, String firstName, String lastName, String linkedIn,
                                        String github, String portfolio, SecurityScenario scenario) throws Exception {
        var requestData = new ModifyAccountProfileRequest(firstName, lastName, linkedIn, github, portfolio);

        var cookie = generateCookieForScenario(scenario, attacker, attacker.getEmail(), secretKey, jwtHelper);

        var request = patch("/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions addJobSite(UUID accountId, String url, SecurityScenario scenario) throws Exception {
        var requestData = new AddJobSiteRequest(url);

        var userAccount = new Account(accountId, USER_EMAIL, USER_PASSWORD);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = post("/profile/job-sites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions removeJobSite(UUID accountId, UUID id, SecurityScenario scenario) throws Exception {
        var userAccount = new Account(accountId, USER_EMAIL, USER_PASSWORD);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);

        var request = delete("/profile/job-sites/" + id)
                .contentType(MediaType.APPLICATION_JSON);

        if (cookie != null) {
            request.cookie(cookie);
        }

        return mockMvc.perform(request);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "SameParameterValue"})
    private void directlyModifyProfile(UUID accountId, String linkedInURL, String githubURL, String portfolioURL) {
        var potentialProfile = accountProfileRepository.findByAccountId(accountId);
        assertThat(potentialProfile).isPresent();

        var existingProfile = potentialProfile.get();
        existingProfile.setLinkedInProfile(linkedInURL);
        existingProfile.setGitHubProfile(githubURL);
        existingProfile.setPortfolioSite(portfolioURL);

        accountProfileRepository.save(existingProfile);

        var updatedProfile = accountProfileRepository.findByAccountId(accountId).get();
        assertThat(updatedProfile.getLinkedInProfile()).isEqualTo(linkedInURL)
                                                       .withFailMessage("Direct LinkedIn update failed!");
        assertThat(updatedProfile.getGitHubProfile()).isEqualTo(githubURL)
                                                     .withFailMessage("Direct GitHub update failed!");
        assertThat(updatedProfile.getPortfolioSite()).isEqualTo(portfolioURL)
                                                     .withFailMessage("Direct Portfolio update failed!");
    }

    private Optional<AccountProfile> getProfileViaUri(UUID accountId, SecurityScenario scenario) throws Exception {
        var userAccount = new Account(accountId, USER_EMAIL, USER_PASSWORD);
        var cookie = generateCookieForScenario(scenario, userAccount, ATTACKER_EMAIL, secretKey, jwtHelper);
        var request = get("/profile");
        if (cookie != null) {
            request.cookie(cookie);
        }

        var response = mockMvc.perform(request).andReturn().getResponse();
        var rootNode = objectMapper.readTree(response.getContentAsString());
        var profileDto = objectMapper.readValue(
                rootNode.traverse(),
                new TypeReference<GetAccountProfileResponse>() {
                });

        if (profileDto.accountId() == null) {
            return Optional.empty();
        }

        var incompleteProfile = new AccountProfile(profileDto.accountId(), profileDto.firstname(), profileDto.surname(),
                profileDto.linkedInProfile(), profileDto.gitHubProfile(), profileDto.portfolioSite());
        profileDto.jobSites().forEach(site ->
                incompleteProfile.addJobSite(new JobSite(site.siteId(), site.url(), site.name())));

        return Optional.of(incompleteProfile);
    }

    private static ResultMatcher noContentHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isNoContent();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> status().isUnauthorized();
        };
    }

    private static ResultMatcher badRequestHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isBadRequest();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> status().isUnauthorized();
        };
    }

    private static ResultMatcher createdHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isCreated();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> status().isUnauthorized();
        };
    }

    private void assertAccountContentsGivenScenario(AccountProfile expectedProfile, List<String> jobSites, MockHttpServletResponse response, SecurityScenario scenario) throws IOException {
        switch (scenario) {
            case VALID_USER -> {
                var actualProfile = accountProfileRepository.findByAccountId(expectedProfile.getAccountId());
                assertThat(actualProfile).isPresent();
                assertThat(actualProfile.get().getFirstName()).isEqualTo(expectedProfile.getFirstName());
                assertThat(actualProfile.get().getLastName()).isEqualTo(expectedProfile.getLastName());
                assertThat(actualProfile.get().getLinkedInProfile()).isEqualTo(expectedProfile.getLinkedInProfile());
                assertThat(actualProfile.get().getGitHubProfile()).isEqualTo(expectedProfile.getGitHubProfile());
                assertThat(actualProfile.get().getPortfolioSite()).isEqualTo(expectedProfile.getPortfolioSite());
            }
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertGenericEndpointResponse(response, scenario);
            default -> fail("Implement the case for " + scenario + "!");
        }
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
}
