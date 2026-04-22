package com.bbmovie.auth.exception;

public class InvalidPasswordException extends RegistrationException {
    public InvalidPasswordException(String message) {
        super(message);
    }
} 