package com.example.bbmovie.exception;

public class CustomEmailException extends RuntimeException {
    public CustomEmailException(String message) {
        super(message);
    }
}
