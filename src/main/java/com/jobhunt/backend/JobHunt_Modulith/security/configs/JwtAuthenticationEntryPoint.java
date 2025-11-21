package com.jobhunt.backend.JobHunt_Modulith.security.configs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String jwtError = (String) request.getAttribute("jwt_error");
        String jwtErrorMessage = (String) request.getAttribute("jwt_error_message");

        String title = "Unauthorized";
        if (jwtError != null) {
            title = jwtError;
        }

        String details = "Authentication required";
        if (jwtError != null) {
            details = jwtErrorMessage;
        }

        String messageJson = """
                {
                  "error": "%s",
                  "details": "%s",
                  "status": "401"
                }
                """.formatted(title, details);

        response.getWriter().write(messageJson);
    }
}
