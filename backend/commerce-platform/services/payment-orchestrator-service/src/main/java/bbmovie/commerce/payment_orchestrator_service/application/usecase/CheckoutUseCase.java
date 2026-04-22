package bbmovie.commerce.payment_orchestrator_service.application.usecase;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutResponse;

public interface CheckoutUseCase {
    CheckoutResponse checkout(String idempotencyKey, CheckoutRequest req);
}

