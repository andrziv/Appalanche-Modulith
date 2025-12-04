package com.appalanche.backend.security.configs;

import com.appalanche.backend.security.helper.JwtHelper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.web.util.WebUtils.getCookie;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtHelper jwtHelper;

    public JwtAuthenticationFilter(JwtHelper jwtHelper) {
        this.jwtHelper = jwtHelper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String jwt = getJwtFromRequest(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String userEmail = jwtHelper.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetails userDetails = User.builder()
                                              .username(userEmail)
                                              .password("")
                                              .authorities(jwtHelper.extractAuthorities(jwt))
                                              .build();

                if (jwtHelper.isTokenValid(jwt, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException exception) {
            logger.warn("Expired token detected for request to: {}", request.getRequestURI());
            logger.debug("Expired token: {}", exception.getMessage());

            request.setAttribute("jwt_error", "Token has expired");
            request.setAttribute("jwt_error_message", exception.getMessage());
        } catch (Exception exception) {
            logger.warn("Invalid token detected for request to: {}", request.getRequestURI());
            logger.debug("Invalid token: {}", exception.getMessage());

            request.setAttribute("jwt_error", "Invalid Token");
            request.setAttribute("jwt_error_message", exception.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        var cookie = getCookie(request, "accessToken");

        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }
}
