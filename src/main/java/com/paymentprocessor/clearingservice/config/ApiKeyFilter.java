package com.paymentprocessor.clearingservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight API-key gate for the REST API, enabled via
 * {@code clearing.security.api-key-enabled=true}. Actuator, OpenAPI and Swagger
 * endpoints remain open for probes and documentation.
 */
@Component
@ConditionalOnProperty(name = "clearing.security.api-key-enabled", havingValue = "true")
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";

    private final String expectedKey;

    public ApiKeyFilter(ClearingProperties properties) {
        this.expectedKey = properties.security().apiKey();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedKey == null || expectedKey.isBlank() || !expectedKey.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
            response.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
                    + "\"detail\":\"Missing or invalid API key\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
