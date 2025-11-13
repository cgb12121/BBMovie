package com.bbmovie.payment.dto;

import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.PaymentProvider;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record PaymentCreatedEvent(
        String userId,
        String userEmail,
        SubscriptionPlan plan,
        BigDecimal amount,
        CurrencyUnit currency,
        PaymentProvider provider,
        String providerTransactionId,
        String description,
        LocalDateTime expiresAt
) {
    public PaymentCreatedEvent(
            String userId,
            String userEmail,
            SubscriptionPlan plan,
            BigDecimal amount,
            CurrencyUnit currency,
            PaymentProvider provider,
            String providerTransactionId,
            String description
    ) {
        this(
                userId,
                userEmail,
                plan,
                amount,
                currency,
                provider,
                providerTransactionId,
                description,
                LocalDateTime.now().plus(Duration.ofMinutes(15))
        );
    }
}
