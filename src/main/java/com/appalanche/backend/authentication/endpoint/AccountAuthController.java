package com.appalanche.backend.authentication.endpoint;

import com.appalanche.backend.authentication.business.AccountService;
import com.appalanche.backend.authentication.business.request_response.LoginRequest;
import com.appalanche.backend.authentication.business.request_response.LoginResponse;
import com.appalanche.backend.authentication.business.request_response.SignupRequest;
import com.appalanche.backend.authentication.persistence.Account;
import com.appalanche.backend.security.helper.JwtHelper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RequestMapping("/authenticate")
@RestController
public class AccountAuthController {
    private final JwtHelper jwtDelegate;
    private final AccountService accountService;

    public AccountAuthController(JwtHelper jwtHelper, AccountService accountService) {
        this.accountService = accountService;
        this.jwtDelegate = jwtHelper;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> register(@Valid @RequestBody SignupRequest request) {
        accountService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        Account authenticatedAccount = accountService.authenticate(request);
        String jwtToken = jwtDelegate.generateToken(authenticatedAccount);

        ResponseCookie jwtCookie = ResponseCookie.from("accessToken", jwtToken)
                                                 .httpOnly(true)
                                                 .secure(false) // TODO: add some sort of nuance to this flag so it's properly required in non-dev enviros
                                                 .path("/")
                                                 .maxAge(jwtDelegate.getExpirationTime() / 1000)
                                                 .sameSite("Strict")
                                                 .build();

        response.addHeader(SET_COOKIE, jwtCookie.toString());

        var responseContent = new LoginResponse(authenticatedAccount.getAccountId(), authenticatedAccount.getEmail());
        return ResponseEntity.ok(responseContent);
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
}
