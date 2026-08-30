package com.buglens.common.security;

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
import java.nio.charset.StandardCharsets;
import java.time.Instant;

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

    /*
     * Written by hand rather than through an injected ObjectMapper. Boot 4 auto-configures the
     * Jackson 3 mapper, so asking for the Jackson 2 type fails context startup outright, and
     * pinning either generation here would couple a security filter to a Jackson upgrade for the
     * sake of four fields. Every value below is a constant defined in this class, so there is
     * nothing to escape — keep it that way, or switch to a real serializer if this ever needs to
     * echo request data back to the caller.
     */
    private void write(HttpServletResponse response, HttpStatus status, String error, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}"
                        .formatted(Instant.now(), status.value(), error, message)
        );
    }
}
