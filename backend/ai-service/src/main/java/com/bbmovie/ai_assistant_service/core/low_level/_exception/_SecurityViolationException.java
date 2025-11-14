package com.bbmovie.ai_assistant_service.core.low_level._exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class _SecurityViolationException extends RuntimeException {
    public _SecurityViolationException(String message) {
        super(message);
    }
}
