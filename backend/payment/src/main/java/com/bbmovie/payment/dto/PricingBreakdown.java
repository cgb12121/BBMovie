package com.bbmovie.payment.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PricingBreakdown(
        BigDecimal base,
        Map<BigDecimal, BigDecimal> cycleDiscount, //rate and flat amount
        Map<BigDecimal, BigDecimal> campaignDiscount, //rate and flat amount
        Map<BigDecimal, BigDecimal> voucherDiscount, //rate and flat amount
        BigDecimal totalDiscount,
        Map<BigDecimal, BigDecimal> tax, //rate and flat amount
        BigDecimal finalPrice
) {}