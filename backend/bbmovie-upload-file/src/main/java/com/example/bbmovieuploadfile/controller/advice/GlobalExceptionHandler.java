package com.example.bbmovieuploadfile.controller.advice;

import com.example.bbmovieuploadfile.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
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

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<String>> handleBindException(WebExchangeBindException ex) {
        return Mono.just(ResponseEntity.badRequest().body("Invalid input: " + ex.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGeneric(Throwable ex) {
        Map<String, Object> errorBody = Map.of(
                ERROR_FIELD, "InternalServerError",
                MESSAGE_FIELD,"Server did not respond in time. Please try again later",
                TIMESTAMP_FIELD, LocalDateTime.now()
        );
        log.error("Unexpected error: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody));
    }
}
