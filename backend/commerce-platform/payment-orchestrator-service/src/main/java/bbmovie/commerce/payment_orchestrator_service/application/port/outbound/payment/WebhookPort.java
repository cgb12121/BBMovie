package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment;

import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.ProviderWebhookEvent;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.WebhookPayload;

public interface WebhookPort extends PaymentProviderPort {
    boolean verifyWebhook(WebhookPayload payload);

    ProviderWebhookEvent parseWebhook(WebhookPayload payload);
}

