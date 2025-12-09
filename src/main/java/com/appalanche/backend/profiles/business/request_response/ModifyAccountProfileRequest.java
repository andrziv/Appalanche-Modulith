package com.appalanche.backend.profiles.business.request_response;

import jakarta.validation.constraints.NotBlank;

public record ModifyAccountProfileRequest(
        @NotBlank(message = "First name cannot be blank")
        String firstname,

        @NotBlank(message = "Last name cannot be blank")
        String surname,

        String linkedInProfileURL,
        String gitHubProfileURL,
        String portfolioSiteURL
) {
}
