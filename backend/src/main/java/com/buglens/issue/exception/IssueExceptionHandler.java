package com.buglens.issue.exception;

import com.buglens.issue.dto.response.IssueErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.issue")
public class IssueExceptionHandler {

    @ExceptionHandler(IssueNotFoundException.class)
    public ResponseEntity<IssueErrorResponse> handleNotFound(IssueNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Issue not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IssueKeyNotFoundException.class)
    public ResponseEntity<IssueErrorResponse> handleKeyNotFound(IssueKeyNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Issue not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IssueComponentNotFoundException.class)
    public ResponseEntity<IssueErrorResponse> handleComponentNotFound(IssueComponentNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Component not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IssueReleaseNotFoundException.class)
    public ResponseEntity<IssueErrorResponse> handleReleaseNotFound(IssueReleaseNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Release not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IssueAssigneeNotFoundException.class)
    public ResponseEntity<IssueErrorResponse> handleAssigneeNotFound(IssueAssigneeNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Assignee not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IssueProjectNotFoundException.class)
    public ResponseEntity<IssueErrorResponse> handleProjectNotFound(IssueProjectNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Project not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IssueAccessDeniedException.class)
    public ResponseEntity<IssueErrorResponse> handleAccessDenied(IssueAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CrossProjectIssueException.class)
    public ResponseEntity<IssueErrorResponse> handleCrossProject(CrossProjectIssueException exception) {
        return error(HttpStatus.BAD_REQUEST, "Cross-project reference", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidIssueFieldException.class)
    public ResponseEntity<IssueErrorResponse> handleInvalidField(InvalidIssueFieldException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid issue field", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<IssueErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<IssueErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<IssueErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new IssueErrorResponse(Instant.now(), status.value(), error, message, fieldErrors)
        );
    }
}
