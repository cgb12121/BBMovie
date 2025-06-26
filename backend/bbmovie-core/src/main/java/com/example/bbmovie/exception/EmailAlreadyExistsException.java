package com.example.bbmovie.exception;

public class EmailAlreadyExistsException extends RegistrationException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
} 