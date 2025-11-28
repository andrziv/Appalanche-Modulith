package com.jobhunt.backend.JobHunt_Modulith;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobhunt.backend.JobHunt_Modulith.authentication.persistence.Account;
import com.jobhunt.backend.JobHunt_Modulith.security.helper.JwtHelper;

import java.io.IOException;
import java.util.Base64;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class SecurityScenarioHelper {
    public enum SecurityScenario {
        VALID_USER, EXPIRED_TOKEN, MALFORMED_TOKEN, MODIFIED_TOKEN, FAKE_TOKEN, NO_TOKEN
    }

    public static String generateHeaderNameForScenario(SecurityScenario scenario) {
        return switch (scenario) {
            case VALID_USER,
                 MALFORMED_TOKEN,
                 EXPIRED_TOKEN,
                 MODIFIED_TOKEN,
                 FAKE_TOKEN -> AUTHORIZATION;
            case NO_TOKEN -> null;
        };
    }

    public static String generateHeaderValueForScenario(SecurityScenario scenario, Account owner, String attackerEmail, String secretKey, JwtHelper jwtHelper) throws IOException {
        return switch (scenario) {
            case VALID_USER -> "Bearer " + jwtHelper.generateToken(owner);
            case MALFORMED_TOKEN -> "Bearer not.a.valid.token";
            case EXPIRED_TOKEN -> {
                var oldHelper = new JwtHelper(secretKey, -1000);
                yield "Bearer " + oldHelper.generateToken(owner);
            }
            case MODIFIED_TOKEN -> {
                var realToken = jwtHelper.generateToken(owner);
                String[] chunks = realToken.split("\\.");
                String header = chunks[0];
                String payload = chunks[1];
                String signature = chunks[2];

                ObjectNode payloadObject = decode(payload);
                payloadObject.put("sub", attackerEmail);

                String illegallyModifiedPayload = encode(payloadObject);
                String illegallyModifiedToken = header + "." + illegallyModifiedPayload + "." + signature;
                yield "Bearer " + illegallyModifiedToken;
            }
            case FAKE_TOKEN -> {
                var fakeKey = rotateString(secretKey, 1, true);
                var fakeHelper = new JwtHelper(fakeKey, 10000);
                yield "Bearer " + fakeHelper.generateToken(owner);
            }
            case NO_TOKEN -> null;
        };
    }

    // https://www.baeldung.com/java-rotate-string-by-n-characters
    @SuppressWarnings("SameParameterValue")
    private static String rotateString(String s, int c, boolean forward) {
        if (c < 0) {
            throw new IllegalArgumentException("Rotation character count cannot be negative!");
        }
        int len = s.length();
        int n = c % len;
        if (n == 0) {
            return s;
        }
        String ss = s + s;

        n = forward ? n : len - n;
        return ss.substring(len - n, 2 * len - n);
    }

    private static ObjectNode decode(String base64) throws IOException {
        return (ObjectNode) new ObjectMapper().readTree(Base64.getUrlDecoder().decode(base64));
    }

    private static String encode(ObjectNode node) throws JsonProcessingException {
        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(new ObjectMapper().writeValueAsBytes(node));
    }
}
