package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment;

import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;

public interface RefundPort extends PaymentProviderPort {
    PaymentResult refund(String orchestratorPaymentId, ProviderPaymentId providerPaymentId);
}

