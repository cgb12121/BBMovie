package com.bbmovie.revenuedashboard.dto;

import java.math.BigDecimal;

public record RevenueByProvider(
        String provider,
        long transactionCount,
        BigDecimal totalAmount,
        BigDecimal refundedAmount,
        double percentOfTotal
) {
}
