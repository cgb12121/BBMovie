package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.CancelSubscriptionRequest;
import com.bbmovie.payment.dto.request.ToggleAutoRenewRequest;
import com.bbmovie.payment.dto.response.UserSubscriptionResponse;
import com.bbmovie.payment.service.UserSubscriptionService;
import com.bbmovie.payment.utils.SimpleJwtDecoder;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscription")
public class UserSubscriptionController {

    private final UserSubscriptionService userSubscriptionService;

    @Autowired
    public UserSubscriptionController(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
    }

    @GetMapping("/mine")
    public ApiResponse<List<UserSubscriptionResponse>> mySubscriptions(@RequestHeader("Authorization") String bearer) {
        String userId = SimpleJwtDecoder.getUserId(bearer);
        return ApiResponse.success(userSubscriptionService.mySubscriptions(userId));
    }

    @PostMapping("/{id}/auto-renew")
    public ApiResponse<UserSubscriptionResponse> toggleAutoRenew(@PathVariable("id") UUID id,
                                                                 @RequestHeader("Authorization") String bearer,
                                                                 @Valid @RequestBody ToggleAutoRenewRequest req) {
        String userId = SimpleJwtDecoder.getUserId(bearer);
        return ApiResponse.success(userSubscriptionService.toggleAutoRenew(id, userId, req));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<UserSubscriptionResponse> cancel(@PathVariable("id") UUID id,
                                                        @RequestHeader("Authorization") String bearer,
                                                        @Valid @RequestBody CancelSubscriptionRequest req) {
        String userId = SimpleJwtDecoder.getUserId(bearer);
        return ApiResponse.success(userSubscriptionService.cancel(id, userId, req));
    }
}


