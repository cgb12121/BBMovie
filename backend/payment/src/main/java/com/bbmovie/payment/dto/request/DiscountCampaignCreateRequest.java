package com.bbmovie.payment.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.*;

public record DiscountCampaignCreateRequest(
        @NotBlank String name,
        @NotBlank String planId,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal discountPercent,
        LocalDateTime startAt,
        LocalDateTime endAt,
        @NotNull Boolean active
) { }


