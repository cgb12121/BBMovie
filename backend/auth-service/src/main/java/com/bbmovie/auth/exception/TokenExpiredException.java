package com.bbmovie.auth.exception;

public class TokenExpiredException extends RegistrationException {
    public TokenExpiredException(String message) {
        super(message);
    }
} 