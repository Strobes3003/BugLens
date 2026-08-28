package com.buglens.project.exception;

import com.buglens.project.dto.response.ProjectErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.buglens.project")
public class ProjectExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ProjectErrorResponse> handleProjectNotFound(ProjectNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Project not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ProjectWorkspaceNotFoundException.class)
    public ResponseEntity<ProjectErrorResponse> handleWorkspaceNotFound(ProjectWorkspaceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Workspace not found", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ProjectAccessDeniedException.class)
    public ResponseEntity<ProjectErrorResponse> handleAccessDenied(ProjectAccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateProjectKeyException.class)
    public ResponseEntity<ProjectErrorResponse> handleDuplicateKey(DuplicateProjectKeyException exception) {
        return error(HttpStatus.CONFLICT, "Duplicate project key", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidProjectKeyException.class)
    public ResponseEntity<ProjectErrorResponse> handleInvalidKey(InvalidProjectKeyException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid project key", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidProjectNameException.class)
    public ResponseEntity<ProjectErrorResponse> handleInvalidName(InvalidProjectNameException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid project name", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProjectErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProjectErrorResponse> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Malformed request", "Request body could not be read", Map.of());
    }

    private ResponseEntity<ProjectErrorResponse> error(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new ProjectErrorResponse(Instant.now(), status.value(), error, message, fieldErrors)
        );
    }
}
