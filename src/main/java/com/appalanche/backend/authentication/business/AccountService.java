package com.appalanche.backend.authentication.business;

import com.appalanche.backend.authentication.business.events.AccountCreationEvent;
import com.appalanche.backend.authentication.business.exceptions.DuplicationException;
import com.appalanche.backend.authentication.business.request_response.LoginRequest;
import com.appalanche.backend.authentication.business.request_response.SignupRequest;
import com.appalanche.backend.authentication.persistence.Account;
import com.appalanche.backend.authentication.persistence.AccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher publisher;

    public AccountService(AccountRepository repository, PasswordEncoder encoder,
                          AuthenticationManager authenticationManager, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.passwordEncoder = encoder;
        this.authenticationManager = authenticationManager;
        this.publisher = publisher;
    }

    @Transactional
    public void signup(SignupRequest request) {
        String email = request.email();
        Optional<Account> existingAccount = repository.findByEmail(email);
        if (existingAccount.isPresent()) {
            throw new DuplicationException(String.format("User with the email address '%s' already exists", email));
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(randomUUID(), email, hashedPassword);
        var createdAccount = repository.save(account);

        publisher.publishEvent(new AccountCreationEvent(createdAccount.getAccountId(), request.firstname(), request.surname()));
    }

    public Account authenticate(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        return repository.findByEmail(request.email()).orElseThrow();
    }

    public Optional<Account> getCurrentUser() {
        return repository.findByAccountId(UUID.fromString(SecurityContextHolder.getContext().getAuthentication()
                                                                               .getName()));
    }
}
