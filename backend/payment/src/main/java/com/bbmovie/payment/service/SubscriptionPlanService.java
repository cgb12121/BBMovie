package com.bbmovie.payment.service;

import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.BillingCycle;
import com.bbmovie.payment.repository.SubscriptionPlanRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static com.bbmovie.payment.entity.enums.BillingCycle.MONTHLY;
import static com.bbmovie.payment.entity.enums.PlanType.*;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

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
                    .baseCurrency("USD")
                    .billingCycle(MONTHLY)
                    .monthlyPrice(BigDecimal.ZERO)
                    .annualDiscountPercent(0)
                    .studentDiscountPercent(0)
                    .active(true)
                    .description("Free plan")
                    .features("Free")
                    .build());
            plans.add(SubscriptionPlan.builder()
                    .name("basic")
                    .planType(BASIC)
                    .baseAmount(new BigDecimal("5.00"))
                    .baseCurrency("USD")
                    .billingCycle(MONTHLY)
                    .monthlyPrice(new BigDecimal("5.00"))
                    .annualDiscountPercent(10)
                    .studentDiscountPercent(20)
                    .active(true)
                    .description("Stream in SD on 1 device")
                    .features("SD,1-device")
                    .build());
            plans.add(SubscriptionPlan.builder()
                    .name("standard")
                    .planType(STANDARD)
                    .baseAmount(new BigDecimal("10.00"))
                    .baseCurrency("USD")
                    .billingCycle(MONTHLY)
                    .monthlyPrice(new BigDecimal("10.00"))
                    .annualDiscountPercent(15)
                    .studentDiscountPercent(25)
                    .active(true)
                    .description("Stream in HD on 2 devices")
                    .features("HD,2-devices")
                    .build());
            plans.add(SubscriptionPlan.builder()
                    .name("premium")
                    .planType(PREMIUM)
                    .baseAmount(new BigDecimal("15.00"))
                    .baseCurrency("USD")
                    .billingCycle(MONTHLY)
                    .monthlyPrice(new BigDecimal("15.00"))
                    .annualDiscountPercent(20)
                    .studentDiscountPercent(30)
                    .active(true)
                    .description("Stream in 4K on 4 devices")
                    .features("4K,4-devices")
                    .build());
            subscriptionPlanRepository.saveAll(plans);
        }
    }

    public List<SubscriptionPlan> listActive() {
        return subscriptionPlanRepository.findAll();
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
            BigDecimal annualDiscount = annualBase.multiply(BigDecimal.valueOf(plan.getAnnualDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = annualBase.subtract(annualDiscount);
        } else {
            price = baseMonthly;
        }
        RestTemplate restTemplate = new RestTemplate();
        boolean isStudent = Boolean.TRUE.equals(restTemplate.getForObject("http://localhost:8080/api/v1/users/" + username, Boolean.class));
        if (isStudent) {
            BigDecimal studentDiscount = price.multiply(BigDecimal.valueOf(plan.getStudentDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = price.subtract(studentDiscount);
        }
        return price.max(BigDecimal.ZERO);
    }
}
