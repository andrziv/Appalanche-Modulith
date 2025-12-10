package com.appalanche.backend.profiles.business.request_response;

import com.appalanche.backend.profiles.persistence.AccountProfile;

import java.util.UUID;

public record GetAccountProfileResponse(
        UUID accountId,
        String firstname,
        String surname,

        String linkedInProfile,
        String gitHubProfile,
        String portfolioSite) {

    public static GetAccountProfileResponse from(AccountProfile profile) {
        return new GetAccountProfileResponse(
                profile.getAccountId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getLinkedInProfile(),
                profile.getGitHubProfile(),
                profile.getPortfolioSite());
    }
}
