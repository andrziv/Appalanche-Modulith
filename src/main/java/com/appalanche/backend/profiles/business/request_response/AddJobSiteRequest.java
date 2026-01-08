package com.appalanche.backend.profiles.business.request_response;

import jakarta.validation.constraints.NotBlank;

public record AddJobSiteRequest(
        @NotBlank(message = "Url should not be blank")
        String url
) {
}
