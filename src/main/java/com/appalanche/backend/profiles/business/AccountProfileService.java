package com.appalanche.backend.profiles.business;

import com.appalanche.backend.profiles.business.request_response.ModifyAccountProfileRequest;
import com.appalanche.backend.profiles.config.KnownJobSiteHostProperties;
import com.appalanche.backend.profiles.persistence.AccountProfileRepository;
import com.appalanche.backend.profiles.persistence.JobSiteRepository;
import com.appalanche.backend.profiles.persistence.dao.AccountProfile;
import com.appalanche.backend.profiles.persistence.dao.JobSite;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.appalanche.backend.profiles.business.UrlHelper.extractSiteName;

@Service
@Transactional(readOnly = true)
public class AccountProfileService {
    private static final Logger logger = LoggerFactory.getLogger(AccountProfileService.class);

    private final AccountProfileRepository profileRepository;
    private final JobSiteRepository siteRepository;

    private final Map<String, String> hostSiteMappings;

    public AccountProfileService(AccountProfileRepository profileRepository, JobSiteRepository siteRepository,
                                 KnownJobSiteHostProperties hostSiteProperties) {
        this.profileRepository = profileRepository;
        this.siteRepository = siteRepository;
        this.hostSiteMappings = new HashMap<>();

        for (KnownJobSiteHostProperties.KnownHostsConfig config : hostSiteProperties.getHosts()) {
            hostSiteMappings.put(config.getHostname(), config.getDisplayName());
        }
    }

    public AccountProfile getProfile() {
        return profileRepository.findByAccountId(getCurrentAccountId())
                                .orElseThrow(() -> new EntityNotFoundException("Profile not found."));
    }

    @Transactional
    public void modifyProfile(ModifyAccountProfileRequest request) {
        logger.info("Received {} to ModifyProfile service method.", request);

        // TODO: FUTURE ME, should I automatically create the profile if it doesn't exist? Kind-of unthinkable that an
        //  account (evidenced by the valid token) exists but the profile doesn't... critical data error?
        var profile = profileRepository.findByAccountId(getCurrentAccountId())
                                       .orElseThrow(() -> new EntityNotFoundException("Profile not found."));


        if (request.firstname() != null) {
            profile.setFirstName(request.firstname());
        }

        if (request.surname() != null) {
            profile.setLastName(request.surname());
        }

        if (request.linkedInProfile() != null) {
            var linkedInSiteURL = request.linkedInProfile();
            var newData = linkedInSiteURL.isBlank() ? null : linkedInSiteURL;
            profile.setLinkedInProfile(newData);
        }

        if (request.gitHubProfile() != null) {
            var gitHubSiteURL = request.gitHubProfile();
            var newData = gitHubSiteURL.isBlank() ? null : gitHubSiteURL;
            profile.setGitHubProfile(newData);
        }

        if (request.portfolioSite() != null) {
            var portfolioSiteURL = request.portfolioSite();
            var newData = portfolioSiteURL.isBlank() ? null : portfolioSiteURL;
            profile.setPortfolioSite(newData);
        }

        profileRepository.save(profile);
    }

    @Transactional
    public void addJobSiteToProfile(String url) {
        var profile = profileRepository.findByAccountId(getCurrentAccountId()).orElseThrow();

        var site = siteRepository.findByUrl(url)
                                 .orElseGet(() -> {
                                     var newSite = new JobSite(url, extractSiteName(url, hostSiteMappings));
                                     return siteRepository.save(newSite);
                                 });

        profile.addJobSite(site);

        profileRepository.save(profile);
    }

    @Transactional
    public void removeJobSite(String url) {
        var profile = profileRepository.findByAccountId(getCurrentAccountId()).orElseThrow();

        for (JobSite jobSite : profile.getJobSites()) {
            if (jobSite.getUrl().equals(url)) {
                profile.removeJobSite(jobSite);
                break;
            }
        }

        profileRepository.save(profile);
    }

    private UUID getCurrentAccountId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
