package com.jobhunt.backend.JobHunt_Modulith.authentication.endpoint;

import com.jobhunt.backend.JobHunt_Modulith.authentication.business.exceptions.DuplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = AccountAuthController.class)
public class AuthenticationExceptionHandler {

    @ExceptionHandler(DuplicationException.class)
    public ResponseEntity<Map<String, String>> handleDuplication(DuplicationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflict", "message", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Login Failed", "message", "Invalid username or password"));
    }
}
