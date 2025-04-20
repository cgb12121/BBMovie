package com.example.bbmovie.exception;

public class BlacklistedJwtTokenException extends RuntimeException {
    public BlacklistedJwtTokenException(String message) {
        super(message);
    }
}
