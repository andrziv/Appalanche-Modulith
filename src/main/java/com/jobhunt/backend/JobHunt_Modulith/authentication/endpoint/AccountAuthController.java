package com.jobhunt.backend.JobHunt_Modulith.authentication.endpoint;

import com.jobhunt.backend.JobHunt_Modulith.authentication.business.*;
import com.jobhunt.backend.JobHunt_Modulith.authentication.helper.JwtHelper;
import com.jobhunt.backend.JobHunt_Modulith.authentication.persistence.Account;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<LoginResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        Account authenticatedAccount = accountService.authenticate(request);
        String jwtToken = jwtDelegate.generateToken(authenticatedAccount);

        LoginResponse response = new LoginResponse(jwtToken, jwtDelegate.getExpirationTime());
        return ResponseEntity.ok(response);
    }
}
