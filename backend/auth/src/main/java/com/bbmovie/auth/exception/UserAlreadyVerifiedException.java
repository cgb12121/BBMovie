package com.bbmovie.auth.exception;

public class UserAlreadyVerifiedException extends RegistrationException {
    public UserAlreadyVerifiedException(String message) {
        super(message);
    }
} 