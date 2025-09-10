package com.bbmovie.payment.dto.request;

import com.bbmovie.payment.entity.enums.BillingCycle;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPaymentRequest(
        @NotBlank String provider,
        @NotNull String subscriptionPlanId,
        @NotNull BillingCycle billingCycle,
        @Nullable String voucherCode
) { }