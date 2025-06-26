package com.example.bbmovie.entity;

import com.example.bbmovie.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subscription_plans")
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