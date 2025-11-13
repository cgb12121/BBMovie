package com.bbmovie.payment.dto;

import com.bbmovie.payment.entity.enums.PlanType;
import lombok.*;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlanView {

    private UUID id;
    private String name;
    private PlanType planType;
    private BigDecimal monthlyPrice;

    private BigDecimal annualOriginalPrice;     // monthly * 12
    private BigDecimal annualDiscountPercent;   // %
    private BigDecimal annualDiscountAmount;    // absolute discount
    private BigDecimal annualFinalPrice;        // after discount

    private String description;
    private String features;
    private CurrencyUnit currency;
}
