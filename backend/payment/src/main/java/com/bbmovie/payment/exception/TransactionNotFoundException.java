package com.bbmovie.payment.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Payment not found");
    }
}