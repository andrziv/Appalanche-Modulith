package com.appalanche.backend.authentication.business.request_response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "First name cannot be blank")
        String firstname,

        @NotBlank(message = "Last name cannot be blank")
        String surname,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email cannot be blank")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters")
        String password) {
}
