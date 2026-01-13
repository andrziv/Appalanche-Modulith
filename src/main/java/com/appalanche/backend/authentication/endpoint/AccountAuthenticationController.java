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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import static com.appalanche.backend.authentication.endpoint.CookieHelper.createJwtCookie;
import static com.appalanche.backend.authentication.endpoint.CookieHelper.createRefreshCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.web.util.WebUtils.getCookie;

@RequestMapping("/authenticate")
@RestController
public class AccountAuthenticationController {
    private final AccountService accountService;

    public AccountAuthenticationController(AccountService accountService) {
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
        ResponseCookie refreshCookie =
                createRefreshCookie(bundle.opaqueRefreshToken(), accountService.getRefreshTokenExpirationTime());

        var authenticatedAccount = bundle.account();
        var responseContent = createLoginResponse(authenticatedAccount);
        return ResponseEntity.ok()
                             .header(SET_COOKIE, jwtCookie.toString())
                             .header(SET_COOKIE, refreshCookie.toString())
                             .body(responseContent);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoginResponse> authenticate() {
        Account authenticatedAccount = accountService.getCurrentUser()
                                                     .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var responseContent = createLoginResponse(authenticatedAccount);
        return ResponseEntity.ok(responseContent);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
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
        ResponseCookie refreshCookie =
                createRefreshCookie(bundle.opaqueRefreshToken(), accountService.getRefreshTokenExpirationTime());

        var responseContent = createLoginResponse(bundle.account());
        return ResponseEntity.ok()
                             .header(SET_COOKIE, jwtCookie.toString())
                             .header(SET_COOKIE, refreshCookie.toString())
                             .body(responseContent);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> invalidateSession(HttpServletRequest request) {
        var incomingRefreshCookie = getCookie(request, "refreshToken");

        String refreshToken = null;
        if (incomingRefreshCookie != null && incomingRefreshCookie.getValue() != null
                && !incomingRefreshCookie.getValue().isBlank()) {
            refreshToken = incomingRefreshCookie.getValue();
        }

        var bundle = accountService.logout(refreshToken);

        ResponseCookie jwtCookie = createJwtCookie(bundle.jwtAccessToken(), 0);

        return ResponseEntity.ok().header(SET_COOKIE, jwtCookie.toString()).build();
    }

    private LoginResponse createLoginResponse(Account account) {
        return new LoginResponse(
                account.getAccountId(), account.getEmail(), accountService.getJwtExpirationTime() * 1000);
    }
}
