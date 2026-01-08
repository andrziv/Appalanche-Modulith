package com.appalanche.backend.profiles.endpoint;

import com.appalanche.backend.profiles.business.AccountProfileService;
import com.appalanche.backend.profiles.business.request_response.AddJobSiteRequest;
import com.appalanche.backend.profiles.business.request_response.GetAccountProfileResponse;
import com.appalanche.backend.profiles.business.request_response.ModifyAccountProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RequestMapping("/profile")
@RestController
public class AccountProfileController {
    private final AccountProfileService profileService;

    public AccountProfileController(AccountProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetAccountProfileResponse> getProfile() {
        var profile = profileService.getProfile();
        return ResponseEntity.ok(GetAccountProfileResponse.from(profile));
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> modifyProfile(@Valid @RequestBody ModifyAccountProfileRequest request) {
        profileService.modifyProfile(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/job-sites")
    public ResponseEntity<Void> addJobSite(@RequestBody @Valid AddJobSiteRequest request) {
        profileService.addJobSiteToProfile(request.url());
        return ResponseEntity.status(CREATED).build();
    }

    @DeleteMapping("/job-sites/{url}")
    public ResponseEntity<Void> removeJobSite(@PathVariable String url) {
        profileService.removeJobSite(url);
        return ResponseEntity.noContent().build();
    }
}
