package com.appalanche.backend.authentication.endpoint;

import org.springframework.http.ResponseCookie;

public class CookieHelper {
    static ResponseCookie createJwtCookie(String jwt, long age) {
        return ResponseCookie.from("accessToken", jwt)
                             .httpOnly(true)
                             .secure(false) // TODO: add some sort of nuance to this flag so it's properly required in non-dev enviros
                             .path("/")
                             .maxAge(age)
                             .sameSite("Strict")
                             .build();
    }

    static ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                             .httpOnly(true)
                             .secure(false) // TODO: add some sort of nuance to this flag so it's properly required in non-dev enviros
                             .path("/authenticate/refresh")
                             .sameSite("Strict")
                             .build();
    }
}
