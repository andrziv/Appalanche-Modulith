package com.appalanche.backend.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.appalanche.backend.applications.business.request_response.AddApplicationRequest;
import com.appalanche.backend.applications.business.request_response.ModifyApplicationRequest;
import com.appalanche.backend.applications.persistence.ApplicationRepository;
import com.appalanche.backend.applications.persistence.JobApplication;
import com.appalanche.backend.applications.persistence.JobApplicationStatus;
import com.appalanche.backend.applications.persistence.JobApplicationStatusRepository;
import com.appalanche.backend.security.helper.JwtHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.*;

import static com.appalanche.backend.SecurityScenarioHelper.*;
import static com.appalanche.backend.SecurityScenarioHelper.SecurityScenario.*;
import static com.appalanche.backend.application.JobApplicationDataHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JobApplicationIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    protected JwtHelper jwtHelper;

    @Autowired
    private JobApplicationStatusRepository statusRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

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
        applicationRepository.deleteAll();
        statusRepository.deleteAll();

        JobApplicationStatus status1 = status1();
        JobApplicationStatus status2 = status2();
        JobApplicationStatus status3 = status3();
        JobApplicationStatus status4 = status4();

        statusRepository.saveAll(List.of(status1, status2, status3, status4));

        applicationRepository.saveAll(
                List.of(firstUserApplication1(status1),
                        firstUserApplication2(status2),
                        firstUserApplication3(status3),
                        secondUserApplication1(status4),
                        secondUserApplication2(status1)));
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldGetAllOwnedJobApplications(SecurityScenario scenario) throws Exception {
        var output = getAllApplications(scenario);

        var response = output.andReturn().getResponse();
        output.andExpect(genericExpectedHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(genericExpectedStatusCodeFor(scenario));
        assertResponseContentForGetAllEndpoint(response, scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldAddNewJobApplicationSuccessfully(SecurityScenario scenario) throws Exception {
        String requisitionId = "R-999";
        String title = "New Title";
        String company = "New Company";
        int interest = 9;
        String statusCode = status1().getCode();
        Date appliedDate = new Date();
        Date responseDate = new Date();

        var output = createApplication(requisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var response = output.andReturn().getResponse();
        output.andExpect(resourceCreatedHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectCreatedStatusCode(scenario));
        assertResponseContentForNewlyCreatedResourceURI(output, scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldModifyExistingJobApplicationSuccessfully(SecurityScenario scenario) throws Exception {
        String newRequisitionId = "R-997";
        String newTitle = "New Title";
        String newCompany = "New Company";
        int newInterest = 9;
        JobApplicationStatus newStatus = status1();
        Date newAppliedDate = dateOffsetBy(1);
        Date newResponseDate = dateOffsetBy(1);
        var oldStatus = statusRepository.save(new JobApplicationStatus("old_status_1", "old status", 0, "000000", "000000"));
        var existingApplication = applicationRepository.save(new JobApplication("0", USER_EMAIL_1, "old title", "old company", 1, oldStatus, null, null));
        var applicationId = existingApplication.getId();

        var output = modifyApplication(applicationId, newRequisitionId, newTitle, newCompany, newInterest, newStatus.getCode(), newAppliedDate, newResponseDate, scenario);

        var response = output.andReturn().getResponse();
        output.andExpect(noContentHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectNoResponseStatusCode(scenario));
        assertApplicationDataUsingId(
                applicationId,
                existingApplication,
                new JobApplication(
                        newRequisitionId,
                        USER_EMAIL_1,
                        newTitle,
                        newCompany,
                        newInterest,
                        newStatus,
                        newAppliedDate,
                        newResponseDate),
                scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldDeleteExistingJobApplicationSuccessfully(SecurityScenario scenario) throws Exception {
        var status = statusRepository.save(new JobApplicationStatus("old_status_1", "old status", 0, "000000", "000000"));
        var existingApplication = applicationRepository.save(new JobApplication("0", USER_EMAIL_1, "old title", "old company", 1, status, null, null));
        var applicationId = existingApplication.getId();

        var output = deleteApplication(applicationId, scenario);

        var response = output.andReturn().getResponse();
        output.andExpect(noContentHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectNoResponseStatusCode(scenario));
        assertMissingApplicationPresence(applicationId, scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailAddingApplicationWithBlankRequisitionIdField(SecurityScenario scenario) throws Exception {
        String blankRequisitionId = " ";
        String title = "New Title";
        String company = "New Company";
        int interest = 9;
        String statusCode = status1().getCode();
        Date appliedDate = new Date();
        Date responseDate = new Date();
        var currentApplicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();

        var output = createApplication(blankRequisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var applicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();
        var response = output.andReturn().getResponse();
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        assertThat(applicationSize).isEqualTo(currentApplicationSize);
        assertFailedValidationContent(response, "{\"requisitionId\":\"Requisition ID cannot be blank\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailAddingApplicationWithBlankTitleField(SecurityScenario scenario) throws Exception {
        String blankRequisitionId = "R-995";
        String title = " ";
        String company = "New Company";
        int interest = 9;
        String statusCode = status1().getCode();
        Date appliedDate = new Date();
        Date responseDate = new Date();
        var currentApplicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();

        var output = createApplication(blankRequisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var applicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();
        var response = output.andReturn().getResponse();
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        assertThat(applicationSize).isEqualTo(currentApplicationSize);
        assertFailedValidationContent(response, "{\"title\":\"Job title cannot be blank\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailAddingApplicationWithBlankCompanyField(SecurityScenario scenario) throws Exception {
        String blankRequisitionId = "R-995";
        String title = "New Title";
        String company = " ";
        int interest = 9;
        String statusCode = status1().getCode();
        Date appliedDate = new Date();
        Date responseDate = new Date();
        var currentApplicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();

        var output = createApplication(blankRequisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var applicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();
        var response = output.andReturn().getResponse();
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        assertThat(applicationSize).isEqualTo(currentApplicationSize);
        assertFailedValidationContent(response, "{\"company\":\"Company name cannot be blank\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailAddingApplicationWithLowerInterestField(SecurityScenario scenario) throws Exception {
        String blankRequisitionId = "R-995";
        String title = "New Title";
        String company = "New Company";
        int interest = 0;
        String statusCode = status1().getCode();
        Date appliedDate = new Date();
        Date responseDate = new Date();
        var currentApplicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();

        var output = createApplication(blankRequisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var applicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();
        var response = output.andReturn().getResponse();
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        assertThat(applicationSize).isEqualTo(currentApplicationSize);
        assertFailedValidationContent(response, "{\"interest\":\"Interest rating must be between 1 and 10, inclusive\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailAddingApplicationWithHighInterestField(SecurityScenario scenario) throws Exception {
        String blankRequisitionId = "R-995";
        String title = "New Title";
        String company = "New Company";
        int interest = 11;
        String statusCode = status1().getCode();
        Date appliedDate = new Date();
        Date responseDate = new Date();
        var currentApplicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();

        var output = createApplication(blankRequisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var applicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();
        var response = output.andReturn().getResponse();
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        assertThat(applicationSize).isEqualTo(currentApplicationSize);
        assertFailedValidationContent(response, "{\"interest\":\"Interest rating must be between 1 and 10, inclusive\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailAddingApplicationWithBlankStatusCodeField(SecurityScenario scenario) throws Exception {
        String blankRequisitionId = "R-995";
        String title = "New Title";
        String company = "New Company";
        int interest = 9;
        String statusCode = " ";
        Date appliedDate = new Date();
        Date responseDate = new Date();
        var currentApplicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();

        var output = createApplication(blankRequisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var applicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();
        var response = output.andReturn().getResponse();
        output.andExpect(badRequestHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectBadRequestStatusCode(scenario));
        assertThat(applicationSize).isEqualTo(currentApplicationSize);
        assertFailedValidationContent(response, "{\"statusCode\":\"Status code cannot be blank\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailAddingApplicationWithUnknownStatusCode(SecurityScenario scenario) throws Exception {
        String blankRequisitionId = "R-995";
        String title = "New Title";
        String company = "New Company";
        int interest = 9;
        String statusCode = "BLAARGH";
        Date appliedDate = new Date();
        Date responseDate = new Date();
        var currentApplicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();

        var output = createApplication(blankRequisitionId, title, company, interest, statusCode, appliedDate, responseDate, scenario);

        var applicationSize = applicationRepository.findByOwnerEmail(USER_EMAIL_1).size();
        var response = output.andReturn().getResponse();
        output.andExpect(resourceMissingHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectResourceMissingStatusCode(scenario));
        assertThat(applicationSize).isEqualTo(currentApplicationSize);
        assertFailedValidationContent(response, "{\"message\":\"Either 'BLAARGH' is an improper status code, or the code was not found in the database.\"}", scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailModifyingApplicationToUseUnknownStatusCode(SecurityScenario scenario) throws Exception {
        var nonexistentStatusCode = "BLAAARGH";
        var appliedDate = dateOffsetBy(1);
        var responseDate = dateOffsetBy(1);
        var oldStatus = statusRepository.save(new JobApplicationStatus("old_status_1", "old status", 0, "000000", "000000"));
        var existingApplication = applicationRepository.save(new JobApplication("0", USER_EMAIL_1, "old title", "old company", 1, oldStatus, appliedDate, responseDate));
        var applicationId = existingApplication.getId();

        var output = modifyApplication(applicationId, null, null, null, null, nonexistentStatusCode, null, null, scenario);

        var response = output.andReturn().getResponse();
        output.andExpect(resourceMissingHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectResourceMissingStatusCode(scenario));
        assertApplicationDataUsingId(
                applicationId,
                existingApplication,
                existingApplication,
                scenario);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailModifyingNonexistentApplication(SecurityScenario scenario) throws Exception {
        String newRequisitionId = "R-997";
        String newTitle = "New Title";
        String newCompany = "New Company";
        int newInterest = 9;
        JobApplicationStatus newStatus = status1();
        Date newAppliedDate = dateOffsetBy(1);
        Date newResponseDate = dateOffsetBy(1);

        var output = modifyApplication(999999, newRequisitionId, newTitle, newCompany, newInterest, newStatus.getCode(), newAppliedDate, newResponseDate, scenario);

        var response = output.andReturn().getResponse();
        output.andExpect(resourceMissingHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectResourceMissingStatusCode(scenario));
        assertFailedValidationContent(response, "{\"message\":\"Job application not found.\"}", scenario);
    }

    @Test
    void shouldFailModifyingOtherUserApplication() throws Exception {
        var status = statusRepository.save(new JobApplicationStatus("old_status_1", "old status", 0, "000000", "000000"));
        var existingApplication = applicationRepository.save(new JobApplication("0", USER_EMAIL_2, "Other title", "Other company", 1, status, null, null));
        var otherUserApplicationId = existingApplication.getId();

        var output = modifyApplication(otherUserApplicationId, "Hacked!", "Hacked!", "Hacked!", null, null, null, null, VALID_USER);

        var response = output.andReturn().getResponse();
        output.andExpect(resourceMissingHttpStatusMatcherFor(VALID_USER));
        assertThat(response.getStatus()).isEqualTo(expectResourceMissingStatusCode(VALID_USER));
        assertFailedValidationContent(response, "{\"message\":\"Job application not found.\"}", VALID_USER);
        assertApplicationDataUsingId(
                otherUserApplicationId,
                existingApplication,
                existingApplication,
                VALID_USER);
    }

    @ParameterizedTest
    @FieldSource("scenarios")
    void shouldFailDeletingNonexistentApplication(SecurityScenario scenario) throws Exception {
        long nonexistentId = 999999;

        var output = deleteApplication(nonexistentId, scenario);

        var response = output.andReturn().getResponse();
        output.andExpect(resourceMissingHttpStatusMatcherFor(scenario));
        assertThat(response.getStatus()).isEqualTo(expectResourceMissingStatusCode(scenario));
        assertThat(applicationRepository.findById(nonexistentId)).isEmpty();
    }

    @Test
    void shouldFailDeletingOtherUserApplication() throws Exception {
        var status = statusRepository.save(new JobApplicationStatus("old_status_1", "old status", 0, "000000", "000000"));
        var existingApplication = applicationRepository.save(new JobApplication("0", USER_EMAIL_2, "Other title", "Other company", 1, status, null, null));
        var otherUserApplicationId = existingApplication.getId();

        var output = deleteApplication(otherUserApplicationId, VALID_USER);

        var response = output.andReturn().getResponse();
        output.andExpect(resourceMissingHttpStatusMatcherFor(VALID_USER));
        assertThat(response.getStatus()).isEqualTo(expectResourceMissingStatusCode(VALID_USER));
        assertFailedValidationContent(response, "{\"message\":\"Job application not found.\"}", VALID_USER);
        assertApplicationDataUsingId(
                otherUserApplicationId,
                existingApplication,
                existingApplication,
                VALID_USER);
    }

    @NotNull
    private ResultActions getAllApplications(SecurityScenario scenario) throws Exception {
        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, firstAccount(), USER_EMAIL_2, secretKey, jwtHelper);

        var request = get("/application").contentType(MediaType.APPLICATION_JSON);

        if (headerName != null) {
            request.header(headerName, headerValue);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions createApplication(String requisitionId, String title, String company, int interestRating,
                                            String statusCode, Date appliedDate, Date responseDate,
                                            SecurityScenario scenario) throws Exception {
        AddApplicationRequest requestData =
                new AddApplicationRequest(requisitionId, title, company, interestRating, statusCode, appliedDate, responseDate);

        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, firstAccount(), USER_EMAIL_2, secretKey, jwtHelper);

        var request = post("/application")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (headerName != null) {
            request.header(headerName, headerValue);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions modifyApplication(long id, String requisitionId, String title, String company,
                                            Integer interestRating, String statusCode, Date appliedDate, Date responseDate,
                                            SecurityScenario scenario) throws Exception {
        ModifyApplicationRequest requestData =
                new ModifyApplicationRequest(requisitionId, title, company, interestRating, statusCode, appliedDate, responseDate);

        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, firstAccount(), USER_EMAIL_2, secretKey, jwtHelper);

        var request = patch("/application/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestData));

        if (headerName != null) {
            request.header(headerName, headerValue);
        }

        return mockMvc.perform(request);
    }

    @NotNull
    private ResultActions deleteApplication(long id, SecurityScenario scenario) throws Exception {
        var headerName = generateHeaderNameForScenario(scenario);
        var headerValue = generateHeaderValueForScenario(scenario, firstAccount(), USER_EMAIL_2, secretKey, jwtHelper);

        var request = delete("/application/" + id)
                .contentType(MediaType.APPLICATION_JSON);

        if (headerName != null) {
            request.header(headerName, headerValue);
        }

        return mockMvc.perform(request);
    }

    private static ResultMatcher resourceCreatedHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isCreated();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> genericExpectedHttpStatusMatcherFor(scenario);
        };
    }

    private static ResultMatcher badRequestHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isBadRequest();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> genericExpectedHttpStatusMatcherFor(scenario);
        };
    }

    private static ResultMatcher resourceMissingHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isNotFound();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> genericExpectedHttpStatusMatcherFor(scenario);
        };
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

    private static ResultMatcher genericExpectedHttpStatusMatcherFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> status().isOk();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> status().isUnauthorized();
        };
    }

    private static int expectCreatedStatusCode(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 201;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> genericExpectedStatusCodeFor(scenario);
        };
    }

    private static int expectBadRequestStatusCode(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 400;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> genericExpectedStatusCodeFor(scenario);
        };
    }

    private static int expectResourceMissingStatusCode(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 404;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> genericExpectedStatusCodeFor(scenario);
        };
    }

    private static int expectNoResponseStatusCode(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 204;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> genericExpectedStatusCodeFor(scenario);
        };
    }

    private static int genericExpectedStatusCodeFor(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER -> 200;
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> 401;
        };
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertResponseContentForGetAllEndpoint(MockHttpServletResponse response, SecurityScenario scenario) throws UnsupportedEncodingException, JsonProcessingException {
        switch (scenario) {
            case VALID_USER -> {
                var applications = objectMapper.readValue(
                        response.getContentAsString(),
                        new TypeReference<List<JobApplication>>() {
                        });
                assertThat(applications)
                        .usingRecursiveComparison()
                        .ignoringFields("id", "appliedDate", "responseDate", "createdAt")
                        .withEqualsForType((status1, status2) ->
                                        status1.getCode().equals(status2.getCode()),
                                JobApplicationStatus.class)
                        .ignoringCollectionOrder()
                        .isEqualTo(List.of(firstUserApplication1(status1()), firstUserApplication2(status2()), firstUserApplication3(status3())));
            }
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertGenericEndpointResponse(response, scenario);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertResponseContentForNewlyCreatedResourceURI(ResultActions result, SecurityScenario scenario) throws Exception {
        var response = result.andReturn().getResponse();

        switch (scenario) {
            case VALID_USER -> {
                result.andExpect(jsonPath("$.id").isNumber());
                String responseBody = response.getContentAsString();
                Integer bodyId = JsonPath.read(responseBody, "$.id");
                String locationHeader = response.getHeader(LOCATION);

                assertThat(locationHeader).endsWith("/application/" + bodyId);
            }
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertGenericEndpointResponse(response, scenario);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertApplicationDataUsingId(long applicationId, JobApplication old, JobApplication expected, SecurityScenario scenario) {
        var applicationInRepository = applicationRepository.findById(applicationId);
        if (applicationInRepository.isEmpty()) {
            fail("The job application with the ID you submitted does not exist. " +
                    "It was either deleted or never existed in the first place, " +
                    "both unacceptable outcomes for a modification endpoint result.");
            return;
        }

        var existingApplication = applicationInRepository.get();

        switch (scenario) {
            case VALID_USER -> assertThat(existingApplication)
                    .usingRecursiveComparison()
                    .ignoringFields("id", "createdAt")
                    .withEqualsForType((status1, status2) ->
                                    status1.getCode().equals(status2.getCode()),
                            JobApplicationStatus.class)
                    .withComparatorForType(Comparator.comparingLong(Date::getTime), Date.class)
                    .ignoringCollectionOrder()
                    .isEqualTo(expected);
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertThat(existingApplication)
                    .usingRecursiveComparison()
                    .ignoringFields("id", "createdAt")
                    .withEqualsForType((status1, status2) ->
                                    status1.getCode().equals(status2.getCode()),
                            JobApplicationStatus.class)
                    .withComparatorForType(Comparator.comparingLong(Date::getTime), java.util.Date.class)
                    .ignoringCollectionOrder()
                    .isEqualTo(old);
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void assertMissingApplicationPresence(long applicationId, SecurityScenario scenario) {
        var applicationInRepository = applicationRepository.findById(applicationId);

        switch (scenario) {
            case VALID_USER -> assertThat(applicationInRepository).isEmpty();
            case MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN,
                 NO_TOKEN -> assertThat(applicationInRepository).isNotEmpty();
            default -> fail("Implement the case for " + scenario + "!");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    @SuppressWarnings("SameParameterValue")
    private static Date dateOffsetBy(int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, hours);

        return calendar.getTime();
    }
}
