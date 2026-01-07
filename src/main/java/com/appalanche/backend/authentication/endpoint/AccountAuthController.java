package com.appalanche.backend.authentication.endpoint;

import com.appalanche.backend.authentication.business.AccountService;
import com.appalanche.backend.authentication.business.exceptions.TokenRefreshException;
import com.appalanche.backend.authentication.business.request_response.LoginRequest;
import com.appalanche.backend.authentication.business.request_response.LoginResponse;
import com.appalanche.backend.authentication.business.request_response.SignupRequest;
import com.appalanche.backend.authentication.persistence.dao.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.web.util.WebUtils.getCookie;

@RequestMapping("/authenticate")
@RestController
public class AccountAuthController {
    private final AccountService accountService;

    public AccountAuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> register(@Valid @RequestBody SignupRequest request) {
        accountService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");
        var bundle = accountService.authenticate(request, userAgent);

        ResponseCookie jwtCookie = createJwtCookie(bundle.jwtAccessToken(), accountService.getJwtExpirationTime());
        ResponseCookie refreshCookie = createRefreshCookie(bundle.opaqueRefreshToken());

        var authenticatedAccount = bundle.account();
        var responseContent = new LoginResponse(authenticatedAccount.getAccountId(), authenticatedAccount.getEmail());
        return ResponseEntity.ok()
                             .header(SET_COOKIE, jwtCookie.toString())
                             .header(SET_COOKIE, refreshCookie.toString())
                             .body(responseContent);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoginResponse> authenticate() {
        Optional<Account> tokenAccount = accountService.getCurrentUser();

        if (tokenAccount.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var authenticatedAccount = tokenAccount.get();
        var responseContent = new LoginResponse(authenticatedAccount.getAccountId(), authenticatedAccount.getEmail());
        return ResponseEntity.ok(responseContent);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        var incomingRefreshCookie = getCookie(request, "refreshToken");

        if (incomingRefreshCookie == null) {
            throw new TokenRefreshException("Refresh cookie is missing");
        }

        var potentialToken = incomingRefreshCookie.getValue();
        if (potentialToken == null || potentialToken.isBlank()) {
            throw new TokenRefreshException("Refresh token is missing");
        }

        String refreshToken = incomingRefreshCookie.getValue();

        var bundle = accountService.refresh(refreshToken);

        ResponseCookie jwtCookie = createJwtCookie(bundle.jwtAccessToken(), accountService.getJwtExpirationTime());
        ResponseCookie refreshCookie = createRefreshCookie(bundle.opaqueRefreshToken());

        return ResponseEntity.ok()
                             .header(SET_COOKIE, jwtCookie.toString())
                             .header(SET_COOKIE, refreshCookie.toString())
                             .build();
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> invalidateSession(HttpServletRequest request) {
        var incomingRefreshCookie = getCookie(request, "refreshToken");

        String refreshToken = null;
        if (incomingRefreshCookie != null
                && (incomingRefreshCookie.getValue() == null || incomingRefreshCookie.getValue().isBlank())) {
            refreshToken = incomingRefreshCookie.getValue();
        }

        var bundle = accountService.logout(refreshToken);

        if (bundle == null) {
            return ResponseEntity.notFound().build();
        }

        ResponseCookie jwtCookie = createJwtCookie(bundle.jwtAccessToken(), 0);

        return ResponseEntity.ok().header(SET_COOKIE, jwtCookie.toString()).build();
    }

    private ResponseCookie createJwtCookie(String jwt, long age) {
        return ResponseCookie.from("accessToken", jwt)
                             .httpOnly(true)
                             .secure(false) // TODO: add some sort of nuance to this flag so it's properly required in non-dev enviros
                             .path("/")
                             .maxAge(age)
                             .sameSite("Strict")
                             .build();
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                             .httpOnly(true)
                             .secure(false) // TODO: add some sort of nuance to this flag so it's properly required in non-dev enviros
                             .path("/authenticate/refresh")
                             .sameSite("Strict")
                             .build();
    }
}
