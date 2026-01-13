package com.appalanche.backend.authentication.endpoint;

import org.springframework.http.ResponseCookie;

public class CookieHelper {
    private static final boolean COOKIE_SECURE = false; // TODO: add some sort of nuance to this flag so it's properly required in non-dev enviros

    static ResponseCookie createJwtCookie(String jwt, long age) {
        return ResponseCookie.from("accessToken", jwt)
                             .httpOnly(true)
                             .secure(COOKIE_SECURE)
                             .path("/")
                             .maxAge(age)
                             .sameSite("Strict")
                             .build();
    }

    static ResponseCookie createRefreshCookie(String refreshToken, long age) {
        return ResponseCookie.from("refreshToken", refreshToken)
                             .httpOnly(true)
                             .secure(COOKIE_SECURE)
                             .path("/authenticate/refresh")
                             .maxAge(age)
                             .sameSite("Strict")
                             .build();
    }
}
