package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.exception;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;

public class UnsupportedProviderCapabilityException extends IllegalArgumentException {
    public UnsupportedProviderCapabilityException(ProviderType type, String capability) {
        super("Provider " + type + " does not support capability: " + capability);
    }
}

