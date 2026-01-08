package com.appalanche.backend.authentication.endpoint;

import com.appalanche.backend.authentication.business.AccountService;
import com.appalanche.backend.authentication.business.request_response.ChangeEmailRequest;
import com.appalanche.backend.authentication.business.request_response.ChangePasswordRequest;
import com.appalanche.backend.authentication.business.request_response.LoginResponse;
import com.appalanche.backend.authentication.persistence.dao.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.appalanche.backend.authentication.endpoint.CookieHelper.createJwtCookie;
import static com.appalanche.backend.authentication.endpoint.CookieHelper.createRefreshCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RequestMapping("/account")
@RestController
public class AccountDetailsController {
    private final AccountService accountService;

    public AccountDetailsController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PatchMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                        HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");

        var bundle = accountService.changePassword(request.oldPassword(), request.newPassword(), userAgent);

        ResponseCookie jwtCookie = createJwtCookie(bundle.jwtAccessToken(), accountService.getJwtExpirationTime());
        ResponseCookie refreshCookie = createRefreshCookie(bundle.opaqueRefreshToken());

        var responseContent = createLoginResponse(bundle.account());
        return ResponseEntity.ok()
                             .header(SET_COOKIE, jwtCookie.toString())
                             .header(SET_COOKIE, refreshCookie.toString())
                             .body(responseContent);
    }

    @PatchMapping("/change-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoginResponse> changeEmail(@Valid @RequestBody ChangeEmailRequest request,
                                                     HttpServletRequest servletRequest) {
        String userAgent = servletRequest.getHeader("User-Agent");

        var bundle = accountService.changeEmail(request.currentPassword(), request.newEmail(), userAgent);

        ResponseCookie jwtCookie = createJwtCookie(bundle.jwtAccessToken(), accountService.getJwtExpirationTime());
        ResponseCookie refreshCookie = createRefreshCookie(bundle.opaqueRefreshToken());

        var responseContent = createLoginResponse(bundle.account());
        return ResponseEntity.ok()
                             .header(SET_COOKIE, jwtCookie.toString())
                             .header(SET_COOKIE, refreshCookie.toString())
                             .body(responseContent);
    }

    private LoginResponse createLoginResponse(Account account) {
        return new LoginResponse(
                account.getAccountId(), account.getEmail(), accountService.getJwtExpirationTime() * 1000);
    }
}
