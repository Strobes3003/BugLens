package com.buglens.comment.exception;

import com.buglens.comment.dto.response.CommentErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.comment")
public class CommentExceptionHandler {

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<CommentErrorResponse> handleNotFound(CommentNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Comment not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CommentIssueNotFoundException.class)
    public ResponseEntity<CommentErrorResponse> handleIssueNotFound(CommentIssueNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Issue not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CommentAuthorNotFoundException.class)
    public ResponseEntity<CommentErrorResponse> handleAuthorNotFound(CommentAuthorNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Comment author not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<CommentErrorResponse> handleAccessDenied(CommentAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidCommentBodyException.class)
    public ResponseEntity<CommentErrorResponse> handleInvalidBody(InvalidCommentBodyException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid comment body", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommentErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommentErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<CommentErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new CommentErrorResponse(Instant.now(), status.value(), error, message, fieldErrors)
        );
    }
}
