package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import com.bbmovie.payment.entity.enums.BillingCycle;
import com.bbmovie.payment.entity.enums.PlanType;
import com.bbmovie.payment.entity.enums.SupportedCurrency;
import com.bbmovie.payment.service.converter.CurrencyUnitAttributeConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Entity
@Table(name = "subscription_plans")
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private PlanType planType;

    @Column(name = "base_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseAmount;

    @Builder.Default
    @Convert(converter = CurrencyUnitAttributeConverter.class)
    @Column(name = "base_currency", nullable = false, length = 3)
    private CurrencyUnit baseCurrency = SupportedCurrency.USD.unit();

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 50)
    private BillingCycle billingCycle;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "features") // consider JSON or separate table
    private String features;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "monthly_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyPrice;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "annual_discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualDiscountPercent;

    @Builder.Default
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "student_discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal studentDiscountPercent = BigDecimal.valueOf(50.0);
}