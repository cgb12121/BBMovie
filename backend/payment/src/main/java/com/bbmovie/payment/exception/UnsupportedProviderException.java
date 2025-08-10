package com.bbmovie.payment.exception;

public class UnsupportedProviderException extends RuntimeException {
    public UnsupportedProviderException(String message) {
        super(message);
    }
}
