package com.appalanche.backend.authentication.endpoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieHelper {
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    ResponseCookie createJwtCookie(String jwt, long age) {
        return ResponseCookie.from("accessToken", jwt)
                             .httpOnly(true)
                             .secure(activeProfile.equals("prod"))
                             .path("/")
                             .maxAge(age)
                             .sameSite("Strict")
                             .build();
    }

    ResponseCookie createRefreshCookie(String refreshToken, long age) {
        return ResponseCookie.from("refreshToken", refreshToken)
                             .httpOnly(true)
                             .secure(activeProfile.equals("prod"))
                             .path("/authenticate/")
                             .maxAge(age)
                             .sameSite("Strict")
                             .build();
    }
}
