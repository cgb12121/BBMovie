package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record CheckoutRequest(
        @NotBlank String userId,
        @Email String userEmail,
        @NotNull ProviderType provider,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String purpose,
        Map<String, String> metadata
) {
}

