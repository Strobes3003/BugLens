package com.buglens.intelligence.exception;

import com.buglens.intelligence.dto.response.IntelligenceErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackages = "com.buglens.intelligence")
public class IntelligenceExceptionHandler {

    @ExceptionHandler(IntelligenceIssueNotFoundException.class)
    public ResponseEntity<IntelligenceErrorResponse> handleIssue(IntelligenceIssueNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "Issue not found", e.getMessage());
    }

    @ExceptionHandler(IntelligenceComponentNotFoundException.class)
    public ResponseEntity<IntelligenceErrorResponse> handleComponent(IntelligenceComponentNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "Component not found", e.getMessage());
    }

    @ExceptionHandler(IntelligenceReleaseNotFoundException.class)
    public ResponseEntity<IntelligenceErrorResponse> handleRelease(IntelligenceReleaseNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "Release not found", e.getMessage());
    }

    @ExceptionHandler(IntelligenceProjectNotFoundException.class)
    public ResponseEntity<IntelligenceErrorResponse> handleProject(IntelligenceProjectNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "Project not found", e.getMessage());
    }

    @ExceptionHandler(IntelligenceAccessDeniedException.class)
    public ResponseEntity<IntelligenceErrorResponse> handleAccessDenied(IntelligenceAccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "Access denied", e.getMessage());
    }

    private ResponseEntity<IntelligenceErrorResponse> error(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                new IntelligenceErrorResponse(Instant.now(), status.value(), error, message)
        );
    }
}
