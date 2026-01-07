package com.appalanche.backend.authentication.business.request_response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
        String currentPassword,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email cannot be blank")
        String newEmail) {
}
