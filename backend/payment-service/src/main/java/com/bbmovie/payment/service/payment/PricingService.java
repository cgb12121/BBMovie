package com.bbmovie.payment.service.payment;

import com.bbmovie.payment.dto.PricingBreakdown;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.BillingCycle;

import javax.money.CurrencyUnit;

public interface PricingService {

    PricingBreakdown calculate(
            SubscriptionPlan plan,
            BillingCycle cycle,
            CurrencyUnit currency,
            String userId,
            String ipAddress,
            String voucherCode
    );

}
