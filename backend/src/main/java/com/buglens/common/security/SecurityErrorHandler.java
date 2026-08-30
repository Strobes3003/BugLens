package com.buglens.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders security failures as JSON and, more importantly, separates the two cases.
 *
 * <p>Without an explicit entry point Spring Security falls back to {@code Http403ForbiddenEntryPoint},
 * so a request carrying no token at all — or an expired one — comes back as 403. That is
 * indistinguishable from a genuine authorization denial, and the client cannot tell that it simply
 * needs to log in again. Unauthenticated now means 401; 403 is reserved for an authenticated caller
 * who lacks access to the resource.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Authentication is required. Sign in and retry with a valid bearer token.");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to access this resource.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String error, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
