package com.bbmovie.gateway.exception;

public class InvalidAuthenticationMethodException extends RuntimeException {
    public InvalidAuthenticationMethodException(String message) {
        super(message);
    }
}
