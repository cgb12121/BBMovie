package com.bbmovie.payment.exception;

import com.paypal.api.payments.Error;

public class PayPalPaymentException extends RuntimeException {
    public PayPalPaymentException(Error error) {
        super(String.valueOf(error));
    }

    public PayPalPaymentException(String message) {
        super(message);
    }
}
