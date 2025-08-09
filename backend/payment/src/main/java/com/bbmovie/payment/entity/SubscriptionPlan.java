package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "base_amount", nullable = false)
    private BigDecimal baseAmount;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle;

    @Column(name = "description")
    private String description;

    @Column(name = "features")
    private String features;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
