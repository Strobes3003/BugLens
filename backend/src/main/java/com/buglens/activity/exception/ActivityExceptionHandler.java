package com.buglens.activity.exception;

import com.buglens.activity.dto.response.ActivityErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackages = "com.buglens.activity")
public class ActivityExceptionHandler {

    @ExceptionHandler(ActivityIssueNotFoundException.class)
    public ResponseEntity<ActivityErrorResponse> handleIssueNotFound(ActivityIssueNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Issue not found", exception.getMessage());
    }

    @ExceptionHandler(ActivityAccessDeniedException.class)
    public ResponseEntity<ActivityErrorResponse> handleAccessDenied(ActivityAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage());
    }

    private ResponseEntity<ActivityErrorResponse> error(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                new ActivityErrorResponse(Instant.now(), status.value(), error, message)
        );
    }
}
