package com.bbmovie.auth.exception;

public class NoRefreshTokenException extends RuntimeException {
    public NoRefreshTokenException(String message) {
        super(message);
    }
}
