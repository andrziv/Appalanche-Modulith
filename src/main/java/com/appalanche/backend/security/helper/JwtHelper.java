package com.appalanche.backend.security.helper;

import com.appalanche.backend.authentication.persistence.dao.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtHelper {
    private final SecretKey secretKey;
    private final long jwtExpiration;

    private final JwtParser jwtParser;

    public JwtHelper(@Value("${security.jwt.secret-key}") String secret,
                     @Value("${security.jwt.expiration-time}") long expiration) {
        this.jwtExpiration = expiration;
        this.secretKey = getSignInKey(secret);

        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    public UUID extractAccountId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);

        List<?> rawList = claims.get("roles", List.class);
        if (rawList == null) {
            rawList = claims.get("authorities", List.class);
        }

        if (rawList == null) {
            return Collections.emptyList();
        }

        return rawList.stream()
                      .filter(Objects::nonNull)
                      .map(Object::toString)
                      .map(SimpleGrantedAuthority::new)
                      .collect(Collectors.toList());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(Account account) {
        return generateToken(new HashMap<>(), account);
    }

    public String generateToken(Map<String, Object> extraClaims, Account account) {
        return buildToken(extraClaims, account, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(Map<String, Object> extraClaims, Account account, long expiration) {
        return Jwts.builder()
                   .claim("email", account.getEmail())
                   .claims(extraClaims)
                   .subject(account.getAccountId().toString())
                   .issuedAt(new Date(System.currentTimeMillis()))
                   .expiration(new Date(System.currentTimeMillis() + expiration))
                   .signWith(secretKey)
                   .compact();
    }

    public boolean isTokenValid(String token, Account account) {
        final UUID accountId = extractAccountId(token);
        return (accountId.equals(account.getAccountId())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    private SecretKey getSignInKey(String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
