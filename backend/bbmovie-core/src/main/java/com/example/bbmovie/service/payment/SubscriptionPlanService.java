package com.example.bbmovie.service.payment;

import com.example.bbmovie.entity.SubscriptionPlan;
import com.example.bbmovie.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public List<SubscriptionPlan> getAllSubscriptionPlans() {
        return subscriptionPlanRepository.findAll();
    }

    public SubscriptionPlan getSubscriptionPlanById(Long id) {
        return subscriptionPlanRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Invalid subscription plan id: " + id));
    }

    public Object getSubscriptionPlanByName(String name) {
        return subscriptionPlanRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subscription plan name: " + name));
    }

    public SubscriptionPlan createSubscriptionPlan(SubscriptionPlan plan) {
        return subscriptionPlanRepository.save(plan);
    }

    public SubscriptionPlan updateSubscriptionPlan(SubscriptionPlan plan) {
        return subscriptionPlanRepository.save(plan);
    }

    public String deleteSubscriptionPlan(String id) {
        subscriptionPlanRepository.deleteById(Long.parseLong(id));
        return "Deleted subscription plan with id: " + id;
    }

    public String disableSubscriptionPlan(String id) {
        subscriptionPlanRepository.updateActiveById(Long.valueOf(id));
        return "Disabled subscription plan with id: " + id;
    }

    public String enableSubscriptionPlan(String id) {
        subscriptionPlanRepository.updateActiveById(Long.valueOf(id));
        return "Enabled subscription plan with id: " + id;
    }
}
