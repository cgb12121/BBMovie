package com.bbmovie.ai_assistant_service.core.low_level._controller._advice;

import com.bbmovie.ai_assistant_service.core.low_level._SessionNotFoundException;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
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
public class _GlobalExceptionHandler {

    private static final _Logger log = _LoggerFactory.getLogger(_GlobalExceptionHandler.class);

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

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        ApiResponse<Void> errorResponse = ApiResponse.error(
                "An unexpected internal error occurred. Please try again later."
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    @ExceptionHandler(SecurityException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleSecurityException(SecurityException ex) {
        log.warn("Security violation: {}", ex.getMessage());

        ApiResponse<Void> errorResponse = ApiResponse.error(ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse));
    }
}
