package com.bbmovie.payment.entity.enums;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

public enum SupportedCurrency {
    USD("USD"),
    EUR("EUR"),
    VND("VND");

    private final CurrencyUnit unit;

    SupportedCurrency(String code) {
        this.unit = Monetary.getCurrency(code);
    }

    public CurrencyUnit unit() {
        return unit;
    }

    public String code() {
        return unit.getCurrencyCode();
    }
}