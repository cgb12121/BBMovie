package com.bbmovie.ai_assistant_service.core.low_level._exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class _InternalServerException extends RuntimeException {
    public _InternalServerException(String message) {
        super(message);
    }
}
