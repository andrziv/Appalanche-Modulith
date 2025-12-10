package com.appalanche.backend.profiles.business.request_response;

import com.appalanche.backend.profiles.business.validation.NullOrNotBlank;
import org.springframework.lang.NonNull;

public record ModifyAccountProfileRequest(
        @NullOrNotBlank(message = "First name cannot be blank if provided")
        String firstname,

        @NullOrNotBlank(message = "Last name cannot be blank if provided")
        String surname,

        String linkedInProfileURL,
        String gitHubProfileURL,
        String portfolioSiteURL) {

    @NonNull
    @Override
    public String toString() {
        return String.format("ModifyAccountProfileRequest[firstname='%s', surname='%s', linkedInProfileURL='%s', " +
                        "gitHubProfileURL='%s', portfolioSiteURL='%s']",
                firstname, surname, linkedInProfileURL, gitHubProfileURL, portfolioSiteURL);
    }
}
