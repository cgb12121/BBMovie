package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.AdminUpsertSubscriptionRequest;
import com.bbmovie.payment.dto.response.SubscriptionAnalyticsResponse;
import com.bbmovie.payment.dto.response.UserSubscriptionResponse;
import com.bbmovie.payment.service.UserSubscriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/subscriptions")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class UserSubscriptionAdminController {

    private final UserSubscriptionService userSubscriptionService;

    @Autowired
    public UserSubscriptionAdminController(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
    }

    @PostMapping
    public ApiResponse<UserSubscriptionResponse> upsert(@Valid @RequestBody AdminUpsertSubscriptionRequest req) {
        return ApiResponse.success(userSubscriptionService.upsert(req));
    }

    @GetMapping("/analytics")
    public ApiResponse<SubscriptionAnalyticsResponse> analytics() {
        return ApiResponse.success(userSubscriptionService.analytics());
    }
}


