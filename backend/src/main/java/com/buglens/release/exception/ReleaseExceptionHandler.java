package com.buglens.release.exception;

import com.buglens.release.dto.response.ReleaseErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.release")
public class ReleaseExceptionHandler {

    @ExceptionHandler(ReleaseNotFoundException.class)
    public ResponseEntity<ReleaseErrorResponse> handleNotFound(ReleaseNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Release not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ReleaseProjectNotFoundException.class)
    public ResponseEntity<ReleaseErrorResponse> handleProjectNotFound(ReleaseProjectNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Project not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ReleaseAccessDeniedException.class)
    public ResponseEntity<ReleaseErrorResponse> handleAccessDenied(ReleaseAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateReleaseVersionException.class)
    public ResponseEntity<ReleaseErrorResponse> handleDuplicateVersion(DuplicateReleaseVersionException exception) {
        return error(HttpStatus.CONFLICT, "Duplicate release version", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidReleaseFieldException.class)
    public ResponseEntity<ReleaseErrorResponse> handleInvalidField(InvalidReleaseFieldException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid release field", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ReleaseErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ReleaseErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<ReleaseErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new ReleaseErrorResponse(Instant.now(), status.value(), error, message, fieldErrors)
        );
    }
}
