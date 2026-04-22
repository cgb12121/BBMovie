package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.SubscriptionPlanView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("unused")
@Tag(name = "Subscription Plans", description = "Subscription plan listing and pricing APIs")
public interface SubscriptionPlanControllerOpenApi {
    @Operation(summary = "List active plans")
    List<SubscriptionPlanView> listPlans();

    @Operation(summary = "Quote subscription price", security = @SecurityRequirement(name = "bearerAuth"))
    BigDecimal quotePrice(
            @RequestParam("plan") String planName,
            @RequestParam(value = "cycle", defaultValue = "monthly") String cycle,
            Authentication authentication
    );
}

