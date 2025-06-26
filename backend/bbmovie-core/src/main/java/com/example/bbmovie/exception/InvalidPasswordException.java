package com.example.bbmovie.exception;

public class InvalidPasswordException extends RegistrationException {
    public InvalidPasswordException(String message) {
        super(message);
    }
} 