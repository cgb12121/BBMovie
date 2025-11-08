package com.bbmovie.ai_assistant_service.core.low_level._controller._advice;

import com.bbmovie.ai_assistant_service.core.low_level._SessionNotFoundException;
import com.example.common.dtos.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class _GlobalErrorHandler {

    @ExceptionHandler(_SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ApiResponse<Void>> handleSessionNotFound(_SessionNotFoundException ex) {
        log.error("[_SessionNotFoundException] Session not found", ex);
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleValidation(ServerWebInputException ex) {
        log.error("[ServerWebInputException] Validation error: {}", ex.getMessage());
        return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage())));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> handleValidation(
            WebExchangeBindException ex) {

        Map<String, String> errors = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        (FieldError e) -> e.getDefaultMessage() != null
                                ? e.getDefaultMessage()
                                : "Invalid value"
                ));
        log.error("[WebExchangeBindException] Validation error: {}", errors);
        return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.validationError(errors)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ApiResponse<Void>> handleResourceNotFound(NoResourceFoundException ex) {
        log.error("Resource not found", ex);
        return Mono.just(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiResponse<Void>> handleGeneric(Throwable ex) {
        log.error("Generic error caught", ex);
        return Mono.just(ApiResponse.error("Internal server error"));
    }
}
