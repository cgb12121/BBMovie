package com.bbmovie.payment.controller;

import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.BillingCycle;
import com.bbmovie.payment.service.SubscriptionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @Autowired
    public SubscriptionPlanController(SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @GetMapping("/plans")
    public List<SubscriptionPlan> listPlans() {
        return subscriptionPlanService.listActive();
    }

    @GetMapping("/")
    public BigDecimal quotePrice(
            @RequestParam("plan") String planName,
            @RequestParam(value = "cycle", defaultValue = "monthly") String cycle,
            Authentication authentication) {
        return subscriptionPlanService.quotePrice(planName, BillingCycle.fromString(cycle), authentication.getName());
    }
}
