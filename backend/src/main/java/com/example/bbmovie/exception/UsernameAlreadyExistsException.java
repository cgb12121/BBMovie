package com.example.bbmovie.exception;

public class UsernameAlreadyExistsException extends RegistrationException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
} 