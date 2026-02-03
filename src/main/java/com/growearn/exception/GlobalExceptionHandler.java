package com.growearn.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", "internal_server_error",
                "message", ex.getMessage(),
                "path", request.getDescription(false)
            )
        );
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex, WebRequest request) {
        return ResponseEntity.status(ex.getStatusCode().value()).body(
            Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "error", ex.getStatusCode().toString().toLowerCase(),
                "message", ex.getReason(),
                "path", request.getDescription(false)
            )
        );
    }
}
