package com.bbmovie.payment.entity.enums;

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

    public String getName() {
        return name.toLowerCase();
    }
}