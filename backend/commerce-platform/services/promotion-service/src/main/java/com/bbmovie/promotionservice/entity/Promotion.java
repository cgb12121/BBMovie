package com.bbmovie.promotionservice.entity;

import com.bbmovie.promotionservice.enums.PromotionStatus;
import com.bbmovie.promotionservice.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PromotionType type;

    @Column(name = "discount_value")
    private Double discountValue; // Percentage or Fixed Amount

    @Column(name = "trial_days")
    private Integer trialDays; // For TRIAL_EXTENSION

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PromotionStatus status;

    @Column(name = "is_automatic", nullable = false)
    private boolean automatic; // True for seasonal rules, False for coupons

    @Column(name = "drools_rule_id")
    private String droolsRuleId; // ID of the rule in drools-engine
}
