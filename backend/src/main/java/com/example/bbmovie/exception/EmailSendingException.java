package com.example.bbmovie.exception;

public class EmailSendingException extends RegistrationException {
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
} 