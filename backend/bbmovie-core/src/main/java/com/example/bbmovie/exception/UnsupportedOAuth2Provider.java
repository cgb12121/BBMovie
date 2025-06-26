package com.example.bbmovie.exception;

public class UnsupportedOAuth2Provider extends RuntimeException {
    public UnsupportedOAuth2Provider(String message) {
        super(message);
    }
}
