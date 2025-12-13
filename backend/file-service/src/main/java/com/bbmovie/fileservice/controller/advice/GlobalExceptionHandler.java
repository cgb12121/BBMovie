package com.bbmovie.fileservice.controller.advice;


import com.bbmovie.fileservice.exception.FileUploadException;
import com.bbmovie.fileservice.exception.UnsupportedExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_FIELD = "error";
    private static final String MESSAGE_FIELD = "message";
    private static final String TIMESTAMP_FIELD = "timestamp";

    @ExceptionHandler(InvalidBearerTokenException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleInvalidBearerTokenException(InvalidBearerTokenException ex) {
        Map<String, Object> errorBody = Map.of(
                ERROR_FIELD, "InvalidBearerTokenError",
                MESSAGE_FIELD, "Invalid access token.",
                TIMESTAMP_FIELD, LocalDateTime.now()
        );
        log.error("Invalid access token: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
    }

    @ExceptionHandler(JwtValidationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleJwtValidationException(JwtValidationException ex) {
        Map<String, Object> errorBody = Map.of(
                ERROR_FIELD, "InvalidJwtTokenError",
                MESSAGE_FIELD, "Invalid JWT token.",
                TIMESTAMP_FIELD, LocalDateTime.now()
        );
        log.error("Invalid JWT token: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
    }

    @ExceptionHandler(FileUploadException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleFileUploadException(FileUploadException ex) {
        Map<String, Object> errorBody = Map.of(
                ERROR_FIELD, "FileUploadError",
                MESSAGE_FIELD, "Error while uploading file.",
                TIMESTAMP_FIELD, LocalDateTime.now()
        );
        log.error("Error while uploading file: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody));
    }

    @ExceptionHandler(SecurityException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleSecurityException(SecurityException ex) {
        Map<String, Object> errorBody = Map.of(
                ERROR_FIELD, "SecurityError",
                MESSAGE_FIELD, "You are not authorized to access this resource",
                TIMESTAMP_FIELD, LocalDateTime.now()
        );
        log.error("Unauthorize: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.error("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<String>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return Mono.just(ResponseEntity.badRequest().body("Invalid path: " + ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedExtension.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleUnsupportedExtension(UnsupportedExtension ex) {
        return Mono.just(ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())));
    }
}
