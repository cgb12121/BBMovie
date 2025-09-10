package com.bbmovie.payment.exception;

public class PaymentNotAvailableException extends RuntimeException {
    public PaymentNotAvailableException(String message) {
        super(message);
    }
}
