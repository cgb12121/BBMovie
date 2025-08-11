package com.bbmovie.payment.entity.enums;

import lombok.Getter;

@Getter
public enum PaymentProvider {
    PAYPAL,
    VNPAY,
    STRIPE,
    ZALOPAY,
    MOMO;

    private final String name;

    PaymentProvider() {
        this.name = this.name().toLowerCase();
    }
}