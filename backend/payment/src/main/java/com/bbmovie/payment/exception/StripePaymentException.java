package com.bbmovie.payment.exception;

public class StripePaymentException extends RuntimeException {
    public StripePaymentException(String message) {
        super(message);
    }
}
