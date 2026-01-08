package com.appalanche.backend.profiles.business.request_response;

import com.appalanche.backend.profiles.persistence.dao.AccountProfile;
import com.appalanche.backend.profiles.persistence.dao.JobSite;

import java.util.List;
import java.util.UUID;

public record GetAccountProfileResponse(
        UUID accountId,
        String firstname,
        String surname,

        String linkedInProfile,
        String gitHubProfile,
        String portfolioSite,

        List<JobSiteDto> jobSites) {

    public static GetAccountProfileResponse from(AccountProfile profile) {
        return new GetAccountProfileResponse(
                profile.getAccountId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getLinkedInProfile(),
                profile.getGitHubProfile(),
                profile.getPortfolioSite(),
                profile.getJobSites().stream().map(JobSiteDto::from).toList());
    }

    public record JobSiteDto(
            UUID siteId,
            String name,
            String url
    ) {
        static JobSiteDto from(JobSite site) {
            return new JobSiteDto(site.getSiteId(), site.getName(), site.getUrl());
        }
    }
}
