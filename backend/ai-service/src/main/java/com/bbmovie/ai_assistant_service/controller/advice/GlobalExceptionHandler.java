package com.bbmovie.ai_assistant_service.controller.advice;

import com.bbmovie.ai_assistant_service.exception.InternalServerException;
import com.bbmovie.ai_assistant_service.exception.SecurityViolationException;
import com.bbmovie.ai_assistant_service.exception.SessionNotFoundException;
import com.bbmovie.ai_assistant_service.utils.log.Logger;
import com.bbmovie.ai_assistant_service.utils.log.LoggerFactory;
import com.bbmovie.common.dtos.ApiResponse;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ApiResponse<Void>> handleSessionNotFound(SessionNotFoundException ex) {
        log.error("[_SessionNotFoundException] Session not found. {}", ex.getMessage());
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

    @ExceptionHandler(SecurityViolationException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleSecurityViolation(SecurityViolationException ex) {
        log.warn("Security violation: {}", ex.getMessage());
        ApiResponse<Void> errorResponse = ApiResponse.error(ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse));
    }

    @ExceptionHandler(InternalServerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiResponse<Void>> handleInternalServer(InternalServerException ex) {
        log.error("Internal server error", ex);
        return Mono.just(ApiResponse.error("Internal server error"));
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiResponse<Void>> handleGeneric(Throwable ex) {
        log.error("Generic error caught", ex);
        return Mono.just(ApiResponse.error("Internal server error"));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        ApiResponse<Void> errorResponse = ApiResponse.error(
                "An unexpected internal error occurred. Please try again later."
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}