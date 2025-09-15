package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.request.AdminUpsertSubscriptionRequest;
import com.bbmovie.payment.dto.request.CancelSubscriptionRequest;
import com.bbmovie.payment.dto.request.ToggleAutoRenewRequest;
import com.bbmovie.payment.dto.response.SubscriptionAnalyticsResponse;
import com.bbmovie.payment.dto.response.UserSubscriptionResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface UserSubscriptionService {
    @Transactional
    void processDueSubscriptions();

    // USER operations
    @Transactional(readOnly = true)
    List<UserSubscriptionResponse> mySubscriptions(String userId);

    @Transactional
    UserSubscriptionResponse toggleAutoRenew(UUID subscriptionId, String userId, ToggleAutoRenewRequest req);

    @Transactional
    UserSubscriptionResponse cancel(UUID subscriptionId, String userId, CancelSubscriptionRequest req);

    // ADMIN operations
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    UserSubscriptionResponse upsert(AdminUpsertSubscriptionRequest req);

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    SubscriptionAnalyticsResponse analytics();
}
