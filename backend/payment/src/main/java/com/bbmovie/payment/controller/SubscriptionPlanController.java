package com.bbmovie.payment.controller;

import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.service.SubscriptionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
