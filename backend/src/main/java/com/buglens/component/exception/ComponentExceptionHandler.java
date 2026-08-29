package com.buglens.component.exception;

import com.buglens.component.dto.response.ComponentErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.component")
public class ComponentExceptionHandler {

    @ExceptionHandler(ComponentNotFoundException.class)
    public ResponseEntity<ComponentErrorResponse> handleNotFound(ComponentNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Component not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ComponentProjectNotFoundException.class)
    public ResponseEntity<ComponentErrorResponse> handleProjectNotFound(ComponentProjectNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Project not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ComponentAccessDeniedException.class)
    public ResponseEntity<ComponentErrorResponse> handleAccessDenied(ComponentAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateComponentNameException.class)
    public ResponseEntity<ComponentErrorResponse> handleDuplicateName(DuplicateComponentNameException exception) {
        return error(HttpStatus.CONFLICT, "Duplicate component name", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidComponentNameException.class)
    public ResponseEntity<ComponentErrorResponse> handleInvalidName(InvalidComponentNameException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid component name", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ComponentErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ComponentErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<ComponentErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new ComponentErrorResponse(Instant.now(), status.value(), error, message, fieldErrors)
        );
    }
}
