package com.jobhunt.backend.JobHunt_Modulith.authentication.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobhunt.backend.JobHunt_Modulith.authentication.business.SignupRequest;
import com.jobhunt.backend.JobHunt_Modulith.authentication.persistence.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class AccountIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Test
    void shouldRegisterNewAccountSuccessfully() throws Exception {
        SignupRequest request = new SignupRequest("Test", "User", "test.user@gmail.com", "1definitely2Secure");

        mockMvc.perform(post("/authenticate/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var account = accountRepository.findByEmail("test.user@gmail.com");
        assertThat(account).isPresent();
        assertThat(account.get().getFirstName()).isEqualTo("Test");
        assertThat(account.get().getLastName()).isEqualTo("User");
        assertThat(account.get().getPassword()).isNotEqualTo("1definitely2Secure");
    }

    @Test
    void shouldFailRegistrationWithInvalidEmailInput() throws Exception {
        // TODO: complete later...
    }

    @Test
    void shouldFailRegistrationWithInvalidPasswordInput() throws Exception {
        // TODO: complete later...
    }

    @Test
    void shouldLoginSuccessfullyWithValidInputs() throws Exception {
        // TODO: complete later...
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        // TODO: complete later...
    }
}
