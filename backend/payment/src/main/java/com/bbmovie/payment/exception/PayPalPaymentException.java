package com.bbmovie.payment.exception;

public class PayPalPaymentException extends RuntimeException {
    public PayPalPaymentException(String message) {
        super(message);
    }
}
