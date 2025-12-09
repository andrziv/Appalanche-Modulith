package com.appalanche.backend.profiles.endpoint;

import com.appalanche.backend.profiles.business.AccountProfileService;
import com.appalanche.backend.profiles.business.request_response.ModifyAccountProfileRequest;
import com.appalanche.backend.profiles.persistence.AccountProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> modifyProfile(@PathVariable UUID accountId,
                                                  @RequestBody ModifyAccountProfileRequest request) {
        profileService.modifyProfile(accountId, request);
        return ResponseEntity.noContent().build();
    }
}
