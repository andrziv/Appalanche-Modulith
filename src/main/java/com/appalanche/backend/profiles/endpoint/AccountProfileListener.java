package com.appalanche.backend.profiles.endpoint;

import com.appalanche.backend.authentication.business.events.AccountCreationEvent;
import com.appalanche.backend.profiles.persistence.dao.AccountProfile;
import com.appalanche.backend.profiles.persistence.AccountProfileRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class AccountProfileListener {
    private final AccountProfileRepository profileRepository;

    public AccountProfileListener(AccountProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @ApplicationModuleListener
    public void on(AccountCreationEvent event) {
        var profile = new AccountProfile(event.accountId(), event.firstName(), event.lastName(),
                null, null, null);

        profileRepository.save(profile);
    }
}
