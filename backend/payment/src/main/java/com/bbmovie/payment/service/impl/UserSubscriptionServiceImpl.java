package com.bbmovie.payment.service.impl;

import com.bbmovie.payment.dto.request.AdminUpsertSubscriptionRequest;
import com.bbmovie.payment.dto.request.CancelSubscriptionRequest;
import com.bbmovie.payment.dto.request.ToggleAutoRenewRequest;
import com.bbmovie.payment.dto.response.SubscriptionAnalyticsResponse;
import com.bbmovie.payment.dto.response.UserSubscriptionResponse;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.UserSubscription;
import com.bbmovie.payment.repository.SubscriptionPlanRepository;
import com.bbmovie.payment.repository.UserSubscriptionRepository;
import com.bbmovie.payment.service.UserSubscriptionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    @Autowired
    public UserSubscriptionServiceImpl(
            UserSubscriptionRepository userSubscriptionRepository,
            SubscriptionPlanRepository planRepository
    ) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    @Override
    public void processDueSubscriptions() {
        log.info("Processing due subscriptions...");
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserSubscriptionResponse> mySubscriptions(String userId) {
        return userSubscriptionRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public UserSubscriptionResponse toggleAutoRenew(UUID subscriptionId, String userId, ToggleAutoRenewRequest req) {
        UserSubscription sub = requireOwnedSubscription(subscriptionId, userId);
        sub.setAutoRenew(Boolean.TRUE.equals(req.autoRenew()));
        return toResponse(userSubscriptionRepository.save(sub));
    }

    @Transactional
    @Override
    public UserSubscriptionResponse cancel(UUID subscriptionId, String userId, CancelSubscriptionRequest req) {
        UserSubscription sub = requireOwnedSubscription(subscriptionId, userId);
        if (Boolean.TRUE.equals(req.immediate())) {
            sub.setActive(false);
            sub.setEndDate(LocalDateTime.now());
        } else {
            // cancel at the period end: keep active until endDate
            sub.setAutoRenew(false);
        }
        return toResponse(userSubscriptionRepository.save(sub));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Override
    public UserSubscriptionResponse upsert(AdminUpsertSubscriptionRequest req) {
        SubscriptionPlan plan = planRepository.findById(req.planId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        UserSubscription sub = userSubscriptionRepository.findByUserId(req.userId())
                .stream().filter(s -> s.getPlan().getId().equals(req.planId()))
                .findFirst()
                .orElseGet(() -> UserSubscription.builder()
                        .userId(req.userId())
                        .plan(plan)
                        .build()
                );

        sub.setActive(Boolean.TRUE.equals(req.active()));
        sub.setAutoRenew(Boolean.TRUE.equals(req.autoRenew()));
        sub.setStartDate(req.startDate());
        sub.setEndDate(req.endDate());
        sub.setLastPaymentDate(req.lastPaymentDate());
        sub.setNextPaymentDate(req.nextPaymentDate());
        sub.setPaymentProvider(req.paymentProvider());
        sub.setPaymentMethod(req.paymentMethod());

        return toResponse(userSubscriptionRepository.save(sub));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Override
    public SubscriptionAnalyticsResponse analytics() {
        long active = userSubscriptionRepository.countByIsActiveTrue();
        long autoRenew = userSubscriptionRepository.countByAutoRenewTrueAndIsActiveTrue();
        long pastEnd = userSubscriptionRepository.countActiveButPastEnd(LocalDateTime.now());
        return SubscriptionAnalyticsResponse.builder()
                .activeSubscribers(active)
                .autoRenewEnabled(autoRenew)
                .pastEndButActive(pastEnd)
                .build();
    }

    // helpers
    private UserSubscription requireOwnedSubscription(UUID id, String userId) {
        UserSubscription sub = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        if (!sub.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Subscription does not belong to user");
        }
        return sub;
    }

    private UserSubscriptionResponse toResponse(UserSubscription s) {
        return UserSubscriptionResponse.builder()
                .id(s.getId())
                .planId(s.getPlan() != null ? s.getPlan().getId() : null)
                .userId(s.getUserId())
                .active(s.isActive())
                .autoRenew(s.isAutoRenew())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .lastPaymentDate(s.getLastPaymentDate())
                .nextPaymentDate(s.getNextPaymentDate())
                .paymentProvider(s.getPaymentProvider())
                .paymentMethod(s.getPaymentMethod())
                .build();
    }
}
