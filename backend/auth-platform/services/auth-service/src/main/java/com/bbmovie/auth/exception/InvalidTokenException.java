package com.bbmovie.auth.exception;

public class InvalidTokenException extends RegistrationException {
    public InvalidTokenException(String message) {
        super(message);
    }
} 