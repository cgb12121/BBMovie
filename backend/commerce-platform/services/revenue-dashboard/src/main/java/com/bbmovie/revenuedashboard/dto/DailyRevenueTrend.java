package com.bbmovie.revenuedashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueTrend(
        LocalDate date,
        BigDecimal revenue,
        long newSubscriptions,
        long cancellations
) {
}
