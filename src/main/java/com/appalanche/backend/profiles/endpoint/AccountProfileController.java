package com.appalanche.backend.profiles.endpoint;

import com.appalanche.backend.profiles.business.AccountProfileService;
import com.appalanche.backend.profiles.business.request_response.ModifyAccountProfileRequest;
import com.appalanche.backend.profiles.persistence.AccountProfile;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/profile")
@RestController
public class AccountProfileController {
    private final AccountProfileService profileService;

    public AccountProfileController(AccountProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountProfile> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> modifyProfile(@Valid @RequestBody ModifyAccountProfileRequest request) {
        profileService.modifyProfile(request);
        return ResponseEntity.noContent().build();
    }
}
