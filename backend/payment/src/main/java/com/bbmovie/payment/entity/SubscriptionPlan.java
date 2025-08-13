package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import com.bbmovie.payment.entity.enums.PlanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Entity
@Table(name = "subscription_plans")
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private PlanType planType;

    @Column(name = "base_amount", nullable = false)
    private BigDecimal baseAmount;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private com.bbmovie.payment.entity.enums.BillingCycle billingCycle;

    @Column(name = "description")
    private String description;

    @Column(name = "features")
    private String features;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "monthly_price", nullable = false)
    private BigDecimal monthlyPrice;

    @Column(name = "annual_discount_percent", nullable = false)
    private int annualDiscountPercent; // 0-100

    @Column(name = "student_discount_percent", nullable = false)
    private int studentDiscountPercent; // 0-100
}
