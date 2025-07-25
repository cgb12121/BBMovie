package com.bbmovie.email.exception;

public class CustomEmailException extends RuntimeException {
    public CustomEmailException(String message) {
        super(message);
    }
}
