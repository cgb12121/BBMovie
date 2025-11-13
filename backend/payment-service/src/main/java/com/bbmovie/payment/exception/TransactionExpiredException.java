package com.bbmovie.payment.exception;

public class TransactionExpiredException extends RuntimeException {
    public TransactionExpiredException(String message) {
        super(message);
    }
}
