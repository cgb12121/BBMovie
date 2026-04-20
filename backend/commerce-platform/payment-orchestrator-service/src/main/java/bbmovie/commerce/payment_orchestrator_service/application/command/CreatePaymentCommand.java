package bbmovie.commerce.payment_orchestrator_service.application.command;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Money;

import java.util.Map;
import java.util.Objects;

/**
 * A normalized command for starting checkout. Keep it provider-agnostic.
 */
public record CreatePaymentCommand(
        String userId,
        String userEmail,
        ProviderType preferredProvider,
        Money amount,
        String purpose,
        Map<String, String> metadata
) {
    public CreatePaymentCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(preferredProvider, "preferredProvider");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(purpose, "purpose");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
    }
}

