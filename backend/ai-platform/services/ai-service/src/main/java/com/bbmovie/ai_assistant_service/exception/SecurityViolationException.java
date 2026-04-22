package com.bbmovie.ai_assistant_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class SecurityViolationException extends RuntimeException {
    public SecurityViolationException(String message) {
        super(message);
    }
}
