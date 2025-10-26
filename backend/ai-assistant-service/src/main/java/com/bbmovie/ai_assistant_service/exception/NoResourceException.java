package com.bbmovie.ai_assistant_service.exception;

public class NoResourceException extends RuntimeException {
    public NoResourceException(String message) {
        super(message);
    }
    public NoResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
