package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment;

public record ProviderCapabilities(
        boolean supportsPaymentCreation,
        boolean supportsRefunds,
        boolean supportsWebhooks,
        boolean supportsRecurring,
        boolean requiresRedirect
) {
}

