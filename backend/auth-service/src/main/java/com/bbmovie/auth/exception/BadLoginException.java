package com.bbmovie.auth.exception;

public class BadLoginException extends RuntimeException {
    public BadLoginException() {
        super("Invalid username or password");
    }
}
