package com.appalanche.backend.profiles.business;

import com.appalanche.backend.profiles.business.request_response.ModifyAccountProfileRequest;
import com.appalanche.backend.profiles.persistence.AccountProfile;
import com.appalanche.backend.profiles.persistence.AccountProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountProfileService {
    private static final Logger logger = LoggerFactory.getLogger(AccountProfileService.class);

    private final AccountProfileRepository profileRepository;

    public AccountProfileService(AccountProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
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

        if (request.linkedInProfileURL() != null) {
            profile.setLinkedInProfile(request.linkedInProfileURL());
        }

        if (request.gitHubProfileURL() != null) {
            profile.setGitHubProfile(request.gitHubProfileURL());
        }

        if (request.portfolioSiteURL() != null) {
            profile.setPortfolioSite(request.portfolioSiteURL());
        }

        profileRepository.save(profile);
    }

    private UUID getCurrentAccountId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
