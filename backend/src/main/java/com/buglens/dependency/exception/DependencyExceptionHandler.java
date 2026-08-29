package com.buglens.dependency.exception;

import com.buglens.dependency.dto.response.DependencyErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.dependency")
public class DependencyExceptionHandler {

    @ExceptionHandler(CycleDetectedException.class)
    public ResponseEntity<DependencyErrorResponse> handleCycle(CycleDetectedException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                new DependencyErrorResponse(
                        Instant.now(),
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Cycle detected",
                        exception.getMessage(),
                        exception.getCyclePath(),
                        Map.of()
                )
        );
    }

    @ExceptionHandler(SelfDependencyException.class)
    public ResponseEntity<DependencyErrorResponse> handleSelfDependency(SelfDependencyException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid dependency", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CrossProjectDependencyException.class)
    public ResponseEntity<DependencyErrorResponse> handleCrossProject(CrossProjectDependencyException exception) {
        return error(HttpStatus.BAD_REQUEST, "Cross-project dependency", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateDependencyException.class)
    public ResponseEntity<DependencyErrorResponse> handleDuplicate(DuplicateDependencyException exception) {
        return error(HttpStatus.CONFLICT, "Duplicate dependency", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DependencyNotFoundException.class)
    public ResponseEntity<DependencyErrorResponse> handleNotFound(DependencyNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Dependency not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DependencyIssueNotFoundException.class)
    public ResponseEntity<DependencyErrorResponse> handleIssueNotFound(DependencyIssueNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Issue not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DependencyProjectNotFoundException.class)
    public ResponseEntity<DependencyErrorResponse> handleProjectNotFound(DependencyProjectNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Project not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DependencyAccessDeniedException.class)
    public ResponseEntity<DependencyErrorResponse> handleAccessDenied(DependencyAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DependencyErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DependencyErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<DependencyErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new DependencyErrorResponse(
                        Instant.now(), status.value(), error, message, List.<Long>of(), fieldErrors
                )
        );
    }
}
