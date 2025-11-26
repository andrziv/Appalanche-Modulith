package com.jobhunt.backend.JobHunt_Modulith;

import com.jobhunt.backend.JobHunt_Modulith.authentication.persistence.Account;
import com.jobhunt.backend.JobHunt_Modulith.security.helper.JwtHelper;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class SecurityScenarioHelper {
    public enum SecurityScenario {
        VALID_USER, WRONG_USER, EXPIRED_TOKEN, MALFORMED_TOKEN, NO_TOKEN
    }

    public static String generateHeaderNameForScenario(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER,
                 WRONG_USER,
                 MALFORMED_TOKEN,
                 EXPIRED_TOKEN -> AUTHORIZATION;
            case NO_TOKEN -> null;
        };
    }

    public static String generateHeaderValueForScenario(SecurityScenario scenario, Account owner, Account otherUser, String secretKey, JwtHelper jwtHelper) {
        return switch (scenario) {
            case VALID_USER -> "Bearer " + jwtHelper.generateToken(owner);
            case WRONG_USER -> "Bearer " + jwtHelper.generateToken(otherUser);
            case MALFORMED_TOKEN -> "Bearer not.a.valid.token";
            case EXPIRED_TOKEN -> {
                var oldHelper = new JwtHelper(secretKey, -1000);
                yield "Bearer " + oldHelper.generateToken(owner);
            }
            case NO_TOKEN -> null;
        };
    }
}
