package com.bbmovie.promotionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionEvaluationContext {
    private UUID userId;
    private String userRole;
    private String userRegion;
    private Double cartValue;
    private String currency;
    private LocalDateTime currentDateTime;
    private String promotionId;
    private String droolsRuleId;
    private boolean eligible;
    private Double appliedDiscountValue;
    private Integer appliedTrialDays;
    private String reason;
}
