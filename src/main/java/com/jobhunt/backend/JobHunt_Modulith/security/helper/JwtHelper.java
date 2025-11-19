package com.jobhunt.backend.JobHunt_Modulith.security.helper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
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

    // Future self: these env variables have dummy "defaults" because building through maven fails without these.
    // For some reason Spring can't read the env file secrets during build? So we just use these.
    // TODO: maybe look into a way that allows the server to fail in case improper values were handed here?
    public JwtHelper(@Value("${security.jwt.secret-key}") String secret,
                     @Value("${security.jwt.expiration-time}") long expiration) {
        this.jwtExpiration = expiration;
        this.secretKey = getSignInKey(secret);

        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
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

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                   .claims(extraClaims)
                   .subject(userDetails.getUsername())
                   .issuedAt(new Date(System.currentTimeMillis()))
                   .expiration(new Date(System.currentTimeMillis() + expiration))
                   .signWith(secretKey)
                   .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
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
