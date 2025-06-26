package com.example.bbmovie.exception;

public class UserAlreadyVerifiedException extends RegistrationException {
    public UserAlreadyVerifiedException(String message) {
        super(message);
    }
} 