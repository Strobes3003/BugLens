package com.buglens.workflow.exception;

import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.exception.IssueAccessDeniedException;
import com.buglens.issue.exception.IssueNotFoundException;
import com.buglens.workflow.dto.response.WorkflowErrorResponse;
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

@RestControllerAdvice(basePackages = "com.buglens.workflow")
public class WorkflowExceptionHandler {

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<WorkflowErrorResponse> handleInvalidTransition(
            InvalidStateTransitionException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                new WorkflowErrorResponse(
                        Instant.now(),
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Invalid state transition",
                        exception.getMessage(),
                        exception.getAllowedTransitions(),
                        Map.of()
                )
        );
    }

    @ExceptionHandler(IssueNotFoundException.class)
    public ResponseEntity<WorkflowErrorResponse> handleNotFound(IssueNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Issue not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IssueAccessDeniedException.class)
    public ResponseEntity<WorkflowErrorResponse> handleAccessDenied(IssueAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WorkflowErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WorkflowErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<WorkflowErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new WorkflowErrorResponse(
                        Instant.now(),
                        status.value(),
                        error,
                        message,
                        List.<IssueStatus>of(),
                        fieldErrors
                )
        );
    }
}
