package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.AdminUpsertSubscriptionRequest;
import com.bbmovie.payment.dto.response.SubscriptionAnalyticsResponse;
import com.bbmovie.payment.dto.response.UserSubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@SuppressWarnings("unused")
@Tag(name = "Subscription Admin", description = "Admin APIs for subscription management")
public interface UserSubscriptionAdminControllerOpenApi {
    @Operation(summary = "Upsert user subscription", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<UserSubscriptionResponse> upsert(@Valid @RequestBody AdminUpsertSubscriptionRequest req);

    @Operation(summary = "Subscription analytics", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<SubscriptionAnalyticsResponse> analytics();
}

