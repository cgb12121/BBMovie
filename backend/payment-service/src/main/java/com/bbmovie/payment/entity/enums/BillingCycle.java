package com.bbmovie.payment.entity.enums;

import lombok.Getter;

@Getter
public enum BillingCycle {
    MONTHLY("monthly"),
    ANNUAL("annual");

    private final String value;

    BillingCycle(String value) {
        this.value = value;
    }

    public static BillingCycle fromString(String value) {
        return valueOf(value.toLowerCase());
    }
}

