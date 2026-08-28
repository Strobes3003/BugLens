package com.buglens.auth.exception;

import com.buglens.auth.dto.response.AuthErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.auth")
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<AuthErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return error(HttpStatus.CONFLICT, "Duplicate email", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AuthErrorResponse> handleBadCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid email or password", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    private ResponseEntity<AuthErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new AuthErrorResponse(Instant.now(), status.value(), error, message, fieldErrors)
        );
    }
}
