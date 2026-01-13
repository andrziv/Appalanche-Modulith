package com.appalanche.backend.authentication.business;

import com.appalanche.backend.authentication.business.events.AccountCreationEvent;
import com.appalanche.backend.authentication.business.exceptions.DuplicationException;
import com.appalanche.backend.authentication.business.exceptions.TokenRefreshException;
import com.appalanche.backend.authentication.business.request_response.LoginRequest;
import com.appalanche.backend.authentication.business.request_response.SignupRequest;
import com.appalanche.backend.authentication.persistence.AccountRepository;
import com.appalanche.backend.authentication.persistence.RefreshTokenRepository;
import com.appalanche.backend.authentication.persistence.dao.Account;
import com.appalanche.backend.authentication.persistence.dao.RefreshToken;
import com.appalanche.backend.security.helper.JwtHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;
    private final RefreshTokenRepository tokenRepository;
    private final JwtHelper jwtDelegate;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher publisher;

    private final int refreshTokenLifetime;
    private final int maxRefreshTokenCount;

    public AccountService(@Value("${security.opaque-token.day-span}") int refreshTokenLifetime,
                          @Value("${security.opaque-token.max-count}") int maxRefreshTokenCount,
                          AccountRepository accountRepository, RefreshTokenRepository tokenRepository,
                          JwtHelper jwtDelegate, PasswordEncoder encoder, AuthenticationManager authenticationManager,
                          ApplicationEventPublisher publisher) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.jwtDelegate = jwtDelegate;
        this.passwordEncoder = encoder;
        this.authenticationManager = authenticationManager;
        this.publisher = publisher;
        this.refreshTokenLifetime = refreshTokenLifetime;
        this.maxRefreshTokenCount = maxRefreshTokenCount;
    }

    @Transactional
    public void signup(SignupRequest request) {
        String email = request.email();
        Optional<Account> existingAccount = accountRepository.findByEmail(email);
        if (existingAccount.isPresent()) {
            throw new DuplicationException(String.format("User with the email address '%s' already exists", email));
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = new Account(randomUUID(), email, hashedPassword);
        var createdAccount = accountRepository.save(account);

        publisher.publishEvent(new AccountCreationEvent(createdAccount.getAccountId(), request.firstname(), request.surname()));
    }

    @Transactional
    public AccountTokenBundle authenticate(LoginRequest request, String deviceName) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Account account = (Account) auth.getPrincipal();

        enforceSessionLimit(account.getAccountId(), deviceName);

        var refreshToken = generateRefreshToken(account, deviceName);
        var savedToken = tokenRepository.save(refreshToken);
        var jwt = jwtDelegate.generateToken(account);

        return new AccountTokenBundle(account, jwt, savedToken.getToken());
    }

    @Transactional
    public AccountTokenBundle refresh(String oldRefreshToken) {
        var refreshToken = tokenRepository.findByToken(oldRefreshToken)
                                          .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));

        if (refreshToken.getExpiryDate().isBefore(now())) {
            tokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token expired, please login again");
        }

        var account = refreshToken.getAccount();

        if (!refreshToken.getAccount().getAccountId().equals(account.getAccountId())) {
            tokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token mismatch, please login again");
        }

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(now().plus(refreshTokenLifetime, DAYS));
        refreshToken.setLastUsed(now());

        RefreshToken savedToken = tokenRepository.save(refreshToken);
        String jwt = jwtDelegate.generateToken(refreshToken.getAccount());

        return new AccountTokenBundle(account, jwt, savedToken.getToken());
    }

    @Transactional
    public AccountTokenBundle logout(String refreshToken) {
        var account = getCurrentUser().orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (refreshToken != null) {
            tokenRepository.findByToken(refreshToken).ifPresent(tokenRepository::delete);
        }

        return new AccountTokenBundle(account, jwtDelegate.generateToken(account), null);
    }

    @Transactional
    public AccountTokenBundle changePassword(String oldPassword, String newPassword, String deviceName) {
        var account = getCurrentUser().orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, account.getPassword())) {
            throw new BadCredentialsException("Current password does not match");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        var tokensToDelete = tokenRepository.findAllByAccount_AccountIdOrderByLastUsedAsc(account.getAccountId());
        for (RefreshToken token : tokensToDelete) {
            tokenRepository.delete(token);
        }

        var refreshToken = generateRefreshToken(account, deviceName);
        var savedToken = tokenRepository.save(refreshToken);
        var jwt = jwtDelegate.generateToken(account);

        return new AccountTokenBundle(account, jwt, savedToken.getToken());
    }

    @Transactional
    public AccountTokenBundle changeEmail(String currentPassword, String newEmail, String deviceName) {
        var account = getCurrentUser().orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new BadCredentialsException("Password incorrect");
        }

        Optional<Account> existingAccount = accountRepository.findByEmail(newEmail);
        if (existingAccount.isPresent()) {
            throw new DuplicationException(String.format("User with the email address '%s' already exists", newEmail));
        }

        account.setEmail(newEmail);
        accountRepository.save(account);

        var tokensToDelete = tokenRepository.findAllByAccount_AccountIdOrderByLastUsedAsc(account.getAccountId());
        for (RefreshToken token : tokensToDelete) {
            tokenRepository.delete(token);
        }

        var refreshToken = generateRefreshToken(account, deviceName);
        var savedToken = tokenRepository.save(refreshToken);
        var jwt = jwtDelegate.generateToken(account);

        return new AccountTokenBundle(account, jwt, savedToken.getToken());
    }

    public Optional<Account> getCurrentUser() {
        return accountRepository.findByAccountId(
                UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName()));
    }

    public long getJwtExpirationTime() {
        return jwtDelegate.getExpirationTime() / 1000;
    }

    public long getRefreshTokenExpirationTime() {
        return (long) refreshTokenLifetime * 24 * 60 * 60;
    }

    private void enforceSessionLimit(UUID accountId, String deviceName) {
        List<RefreshToken> sessions = tokenRepository.findAllByAccount_AccountIdOrderByLastUsedAsc(accountId);

        if (sessions.size() >= maxRefreshTokenCount) {
            int itemsToDelete = sessions.size() - maxRefreshTokenCount + 1;
            for (RefreshToken token : new ArrayList<>(sessions)) {
                if (token.getDeviceName().equals(deviceName)) {
                    tokenRepository.delete(token);
                    sessions.remove(token);
                    itemsToDelete--;
                }
            }

            for (int i = 0; i < itemsToDelete; i++) {
                tokenRepository.delete(sessions.get(i));
            }
        }
    }

    private RefreshToken generateRefreshToken(Account account, String deviceName) {
        return new RefreshToken(account, randomUUID(), randomUUID().toString(), deviceName, now().plus(refreshTokenLifetime, DAYS), now());
    }

    public record AccountTokenBundle(
            Account account,
            String jwtAccessToken,
            String opaqueRefreshToken
    ) {
    }
}
