package com.bbmovie.payment.service.payment.provider.paypal;

import lombok.Getter;

@Getter
public enum PaypalTransactionStatus {
    APPROVED("APPROVED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String status;

    PaypalTransactionStatus(String status) {
        this.status = status;
    }
}
