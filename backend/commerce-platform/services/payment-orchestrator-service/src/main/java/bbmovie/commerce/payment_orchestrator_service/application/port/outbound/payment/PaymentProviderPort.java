package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;

public interface PaymentProviderPort {
    ProviderType type();

    ProviderCapabilities capabilities();
}

