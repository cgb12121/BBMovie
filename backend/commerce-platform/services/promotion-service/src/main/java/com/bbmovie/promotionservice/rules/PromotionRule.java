package com.bbmovie.promotionservice.rules;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PromotionRule {
    private String ruleId;
    private String name;
    private UUID promotionId;
    private String planId;
    private Double minCartValue;
    private String userRole;
    private String userRegion;
    private String currency;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean oneTimePerUser = true;
    private Double discountValue;
    private Integer trialDays;
}
