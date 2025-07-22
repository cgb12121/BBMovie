package com.bbmovie.auth.exception;

public class UsernameAlreadyExistsException extends RegistrationException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
} 