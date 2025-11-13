package com.bbmovie.payment.dto.request;

import com.bbmovie.payment.entity.enums.VoucherType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherCreateRequest(
        @NotBlank String code,
        @NotNull VoucherType type,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal percentage,
        @DecimalMin("0.0") BigDecimal amount,
        String userSpecificId,
        boolean permanent,
        LocalDateTime startAt,
        LocalDateTime endAt,
        @Min(1) int maxUsePerUser,
        boolean active
) {}


