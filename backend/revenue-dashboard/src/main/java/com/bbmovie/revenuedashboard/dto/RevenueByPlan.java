package com.bbmovie.revenuedashboard.dto;

import java.math.BigDecimal;

public record RevenueByPlan(
        String planType,
        long subscriptionCount,
        BigDecimal mrr,
        BigDecimal totalRevenue,
        double percentOfTotal
) {
}
