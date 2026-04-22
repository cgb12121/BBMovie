package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.config.payment.PaymentProviderRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@SuppressWarnings("unused")
@Tag(name = "Payment Admin", description = "Administrative operations for payment providers")
public interface PaymentAdminControllerOpenApi {
    @Operation(summary = "Enable provider", security = @SecurityRequirement(name = "bearerAuth"))
    void enable(@PathVariable String provider, @RequestParam String reason, Authentication authentication);

    @Operation(summary = "Disable provider", security = @SecurityRequirement(name = "bearerAuth"))
    void disable(@PathVariable String provider, @RequestParam String reason, Authentication authentication);

    @Operation(summary = "Get provider status", security = @SecurityRequirement(name = "bearerAuth"))
    PaymentProviderRegistry.ProviderStatus status(@PathVariable String provider, Authentication authentication);
}

