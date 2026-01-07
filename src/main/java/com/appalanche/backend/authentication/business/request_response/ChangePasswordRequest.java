package com.appalanche.backend.authentication.business.request_response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        String oldPassword,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters")
        String newPassword) {
}
