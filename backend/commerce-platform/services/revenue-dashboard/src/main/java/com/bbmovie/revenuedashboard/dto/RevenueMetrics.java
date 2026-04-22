package com.bbmovie.revenuedashboard.dto;

import java.math.BigDecimal;

public record RevenueMetrics(
        // Core metrics
        BigDecimal mrr,                // Monthly Recurring Revenue
        BigDecimal arr,                // Annual Recurring Revenue (MRR * 12)
        BigDecimal totalRevenueToday,
        BigDecimal totalRevenueThisMonth,

        // Subscription counts
        long activeSubscriptions,
        long newSubscriptionsToday,
        long cancelledSubscriptionsToday,
        long renewedSubscriptionsToday,

        // Churn
        double churnRatePercent,       // cancellations / active subscriptions (last 30 days)

        // Averages
        BigDecimal arpu,              // Average Revenue Per User

        // Period-over-period growth
        double mrrGrowthPercent        // MRR this month vs last month
) {
    public static RevenueMetrics empty() {
        return new RevenueMetrics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, 0, 0,
                0.0,
                BigDecimal.ZERO,
                0.0
        );
    }
}
