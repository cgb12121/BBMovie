package com.bbmovie.payment.dto;

import java.math.BigDecimal;

public record DiscountResult(BigDecimal rate, BigDecimal amount) {
    public static DiscountResult none() {
        return new DiscountResult(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
