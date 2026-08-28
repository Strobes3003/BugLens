package com.buglens.workspace.exception;

import com.buglens.workspace.dto.response.WorkspaceErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.workspace")
public class WorkspaceExceptionHandler {

    @ExceptionHandler(WorkspaceNotFoundException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleNotFound(WorkspaceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Workspace not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(WorkspaceMemberNotFoundException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleMemberNotFound(WorkspaceMemberNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Workspace member not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(WorkspaceUserNotFoundException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleUserNotFound(WorkspaceUserNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "User not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(WorkspaceAccessDeniedException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleAccessDenied(WorkspaceAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(WorkspaceConflictException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleConflict(WorkspaceConflictException exception) {
        return error(HttpStatus.CONFLICT, "Workspace conflict", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidWorkspaceRoleException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleInvalidRole(InvalidWorkspaceRoleException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid workspace role", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WorkspaceErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<WorkspaceErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new WorkspaceErrorResponse(Instant.now(), status.value(), error, message, fieldErrors)
        );
    }
}
