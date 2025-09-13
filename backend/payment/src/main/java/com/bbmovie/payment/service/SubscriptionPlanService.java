package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.SubscriptionPlanView;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.BillingCycle;
import com.bbmovie.payment.repository.SubscriptionPlanRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.RoundingMode;
import java.util.*;
import java.math.BigDecimal;

import static com.bbmovie.payment.entity.enums.BillingCycle.MONTHLY;
import static com.bbmovie.payment.entity.enums.PlanType.*;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @PostConstruct
    private void initDefaultSubscriptionPlan() {
        // Seed fixed plans if not present
        if (subscriptionPlanRepository.count() == 0) {
            List<SubscriptionPlan> plans = new ArrayList<>();
            plans.add(SubscriptionPlan.builder()
                    .name("free")
                    .planType(FREE)
                    .baseAmount(BigDecimal.ZERO)
                    .billingCycle(MONTHLY)
                    .monthlyPrice(BigDecimal.ZERO)
                    .annualDiscountPercent(BigDecimal.valueOf(0))
                    .studentDiscountPercent(BigDecimal.valueOf(0))
                    .active(true)
                    .description("Free plan")
                    .features("Free")
                    .build());
            plans.add(SubscriptionPlan.builder()
                    .name("basic")
                    .planType(BASIC)
                    .baseAmount(BigDecimal.valueOf(5))
                    .billingCycle(MONTHLY)
                    .monthlyPrice(BigDecimal.valueOf(5))
                    .annualDiscountPercent(BigDecimal.valueOf(10))
                    .studentDiscountPercent(BigDecimal.valueOf(20))
                    .active(true)
                    .description("Stream in SD on 1 device")
                    .features("SD,1-device")
                    .build());
            plans.add(SubscriptionPlan.builder()
                    .name("standard")
                    .planType(STANDARD)
                    .baseAmount(BigDecimal.valueOf(10))
                    .billingCycle(MONTHLY)
                    .monthlyPrice(BigDecimal.valueOf(10))
                    .annualDiscountPercent(BigDecimal.valueOf(15))
                    .studentDiscountPercent(BigDecimal.valueOf(25))
                    .active(true)
                    .description("Stream in HD on 2 devices")
                    .features("HD,2-devices")
                    .build());
            plans.add(SubscriptionPlan.builder()
                    .name("premium")
                    .planType(PREMIUM)
                    .baseAmount(BigDecimal.valueOf(15))
                    .billingCycle(MONTHLY)
                    .monthlyPrice(BigDecimal.valueOf(15))
                    .annualDiscountPercent(BigDecimal.valueOf(20))
                    .studentDiscountPercent(BigDecimal.valueOf(30))
                    .active(true)
                    .description("Stream in 4K on 4 devices")
                    .features("4K,4-devices")
                    .build());
            subscriptionPlanRepository.saveAll(plans);
        }
    }

    public SubscriptionPlan getById(UUID id) {
        return subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found..."));
    }

    public void createPlan(SubscriptionPlan plan) {
        subscriptionPlanRepository.save(plan);
    }

    //request update dto missing
    public void updatePlan(UUID id) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(id)
                .orElseThrow();
        subscriptionPlanRepository.save(subscriptionPlan);
    }

    public void  activePlan(UUID id) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found..."));
        if (!subscriptionPlan.isActive()) {
            subscriptionPlan.setActive(true);
            subscriptionPlanRepository.save(subscriptionPlan);
        }
        throw new IllegalArgumentException("Plan is already active");
    }

    public void inactivePlan(UUID id) {
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        if (subscriptionPlan.isActive()) {
            subscriptionPlan.setActive(false);
            subscriptionPlanRepository.save(subscriptionPlan);
        }
        throw new IllegalArgumentException("Plan is already inactive");
    }

    public void deleteById(UUID id) {
        subscriptionPlanRepository.deleteById(id);
    }

    public BigDecimal quotePrice(String planName, BillingCycle cycle, String username) {
        Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findAll()
                .stream()
                .filter(p -> p.isActive() && p.getName().equalsIgnoreCase(planName))
                .findFirst();
        SubscriptionPlan plan = planOpt.orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        BigDecimal baseMonthly = plan.getMonthlyPrice();
        boolean annual = cycle == BillingCycle.ANNUAL;
        BigDecimal price;
        if (annual) {
            BigDecimal annualBase = baseMonthly.multiply(BigDecimal.valueOf(12));
            BigDecimal annualDiscount = annualBase.multiply(plan.getAnnualDiscountPercent())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = annualBase.subtract(annualDiscount);
        } else {
            price = baseMonthly;
        }

        boolean isStudent = Boolean.TRUE.equals(restTemplate.getForObject(
                "http://localhost:8080/api/student-program/status", Boolean.class, Map.of("username", username))
        );
        if (isStudent) {
            BigDecimal studentDiscount = price.multiply(plan.getStudentDiscountPercent())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = price.subtract(studentDiscount);
        }
        return price.max(BigDecimal.ZERO);
    }

    public List<SubscriptionPlanView> listActivePlans() {
        return subscriptionPlanRepository.findAll()
                .stream()
                .filter(SubscriptionPlan::isActive)
                .map(this::mapToView)
                .toList();
    }

    private SubscriptionPlanView mapToView(SubscriptionPlan plan) {
        BigDecimal monthlyPrice = plan.getMonthlyPrice();

        // Annual values
        BigDecimal annualOriginal = monthlyPrice.multiply(BigDecimal.valueOf(12));
        BigDecimal discountAmount = annualOriginal
                .multiply(plan.getAnnualDiscountPercent())
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        BigDecimal annualFinal = annualOriginal.subtract(discountAmount);

        return SubscriptionPlanView.builder()
                .id(plan.getId())
                .name(plan.getName())
                .planType(plan.getPlanType())
                .monthlyPrice(monthlyPrice)
                .annualOriginalPrice(annualOriginal)
                .annualDiscountPercent(plan.getAnnualDiscountPercent())
                .annualDiscountAmount(discountAmount)
                .annualFinalPrice(annualFinal)
                .description(plan.getDescription())
                .features(plan.getFeatures())
                .currency(plan.getBaseCurrency())
                .build();
    }
}