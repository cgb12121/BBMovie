package com.bbmovie.payment.entity.enums;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Getter
@Log4j2
public enum PaymentStatus {
    PENDING("PENDING"),
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    AUTO_CANCELLED("AUTO_CANCELLED"),
    REFUNDED("REFUNDED");

    private final String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    public record NormalizedPaymentStatus(PaymentStatus status, String message) {

    }
}   