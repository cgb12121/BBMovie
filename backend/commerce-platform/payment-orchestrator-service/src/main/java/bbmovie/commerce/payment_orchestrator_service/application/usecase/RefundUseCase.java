package bbmovie.commerce.payment_orchestrator_service.application.usecase;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundResponse;

public interface RefundUseCase {
    RefundResponse refund(String idempotencyKey, RefundRequest req);
}

