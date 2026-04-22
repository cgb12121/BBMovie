package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.CancelSubscriptionRequest;
import com.bbmovie.payment.dto.request.ToggleAutoRenewRequest;
import com.bbmovie.payment.dto.response.UserSubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "User Subscriptions", description = "User subscription management APIs")
public interface UserSubscriptionControllerOpenApi {
    @Operation(summary = "Get my subscriptions", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<UserSubscriptionResponse>> mySubscriptions(@RequestHeader("Authorization") String bearer);

    @Operation(summary = "Toggle auto-renew", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<UserSubscriptionResponse> toggleAutoRenew(
            @PathVariable("id") UUID id,
            @RequestHeader("Authorization") String bearer,
            @Valid @RequestBody ToggleAutoRenewRequest req
    );

    @Operation(summary = "Cancel subscription", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<UserSubscriptionResponse> cancel(
            @PathVariable("id") UUID id,
            @RequestHeader("Authorization") String bearer,
            @Valid @RequestBody CancelSubscriptionRequest req
    );
}

