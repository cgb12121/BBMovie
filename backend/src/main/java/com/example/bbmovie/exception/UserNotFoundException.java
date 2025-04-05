package com.example.bbmovie.exception;

public class UserNotFoundException extends RegistrationException {
    public UserNotFoundException(String message) {
        super(message);
    }
} 