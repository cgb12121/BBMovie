package com.example.bbmovie.exception;

public class TokenExpiredException extends RegistrationException {
    public TokenExpiredException(String message) {
        super(message);
    }
} 