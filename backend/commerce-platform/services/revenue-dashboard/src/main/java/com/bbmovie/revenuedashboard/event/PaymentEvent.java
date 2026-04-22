package com.bbmovie.revenuedashboard.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        String eventType,        // payment.completed, payment.refunded, subscription.created, subscription.cancelled, subscription.renewed
        String transactionId,
        String userId,
        String subscriptionId,
        String planId,
        String planType,         // FREE, PREMIUM, FAMILY, etc.
        BigDecimal amount,
        String currency,
        String provider,         // STRIPE, PAYPAL, VNPAY, ZALOPAY, MOMO
        String billingCycle,     // MONTHLY, ANNUAL
        LocalDateTime eventTimestamp
) {
}
