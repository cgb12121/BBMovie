package com.bbmovie.auth.exception;

public class BlacklistedJwtTokenException extends RuntimeException {
    public BlacklistedJwtTokenException(String message) {
        super(message);
    }
}
