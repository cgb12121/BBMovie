package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import bbmovie.commerce.payment_orchestrator_service.application.exception.IdempotencyConflictException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<?> idempotencyConflict(IdempotencyConflictException e) {
        log.warn("Idempotency conflict", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "PAYMENT_IDEMPOTENCY_CONFLICT",
                "message", "Request conflicts with existing idempotency key"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException e) {
        log.warn("Bad request", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "code", "BAD_REQUEST",
                "message", "Invalid request input"
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        log.warn("Validation error", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "code", "VALIDATION_ERROR",
                "message", "Invalid request"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unknown(Exception e) {
        log.error("Unhandled server exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", "INTERNAL_SERVER_ERROR",
                "message", "Unexpected server error"
        ));
    }
}

