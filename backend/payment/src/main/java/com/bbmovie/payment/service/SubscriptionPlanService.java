package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.SubscriptionPlanView;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.BillingCycle;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanService {
    SubscriptionPlan getById(UUID id);

    void createPlan(SubscriptionPlan plan);

    //request update dto missing
    void updatePlan(UUID id);

    void  activePlan(UUID id);

    void inactivePlan(UUID id);

    void deleteById(UUID id);

    BigDecimal quotePrice(String planName, BillingCycle cycle, String username);

    List<SubscriptionPlanView> listActivePlans();
}
