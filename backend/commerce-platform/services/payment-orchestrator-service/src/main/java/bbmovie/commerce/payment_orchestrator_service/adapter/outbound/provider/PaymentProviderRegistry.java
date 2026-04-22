package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.PaymentCreationPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.PaymentProviderPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.RefundPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.WebhookPort;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.exception.UnsupportedProviderCapabilityException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProviderRegistry {

    private final Map<ProviderType, PaymentProviderPort> providers = new EnumMap<>(ProviderType.class);

    public PaymentProviderRegistry(List<PaymentProviderPort> providerList) {
        for (PaymentProviderPort p : providerList) {
            providers.put(p.type(), p);
        }
    }

    public PaymentProviderPort getRequired(ProviderType type) {
        PaymentProviderPort p = providers.get(type);
        if (p == null) {
            throw new IllegalArgumentException("Unsupported provider: " + type);
        }
        return p;
    }

    public PaymentCreationPort getRequiredCreationProvider(ProviderType type) {
        PaymentProviderPort provider = getRequired(type);
        if (provider instanceof PaymentCreationPort creationProvider
                && provider.capabilities().supportsPaymentCreation()) {
            return creationProvider;
        }
        throw new UnsupportedProviderCapabilityException(type, "payment_creation");
    }

    public RefundPort getRequiredRefundProvider(ProviderType type) {
        PaymentProviderPort provider = getRequired(type);
        if (provider instanceof RefundPort refundProvider
                && provider.capabilities().supportsRefunds()) {
            return refundProvider;
        }
        throw new UnsupportedProviderCapabilityException(type, "refund");
    }

    public WebhookPort getRequiredWebhookProvider(ProviderType type) {
        PaymentProviderPort provider = getRequired(type);
        if (provider instanceof WebhookPort webhookProvider
                && provider.capabilities().supportsWebhooks()) {
            return webhookProvider;
        }
        throw new UnsupportedProviderCapabilityException(type, "webhook");
    }
}

