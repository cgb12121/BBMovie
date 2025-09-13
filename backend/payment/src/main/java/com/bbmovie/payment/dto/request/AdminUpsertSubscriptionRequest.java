package com.bbmovie.payment.dto.request;

import com.bbmovie.payment.entity.enums.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUpsertSubscriptionRequest(
        @NotBlank String userId,
        @NotNull UUID planId,
        @NotNull Boolean active,
        @NotNull Boolean autoRenew,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime lastPaymentDate,
        LocalDateTime nextPaymentDate,
        PaymentProvider paymentProvider,
        String paymentMethod
) {}


