package com.bbmovie.auth.exception;

public class EmailSendingException extends RegistrationException {
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
} 