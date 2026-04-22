package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundResponse;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.RefundUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class RefundController {

    private final RefundUseCase refundOrchestrationService;

    @PostMapping("/refunds")
    public RefundResponse refund(
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody RefundRequest req
    ) {
        log.info("Refund request: {}", req);
        return refundOrchestrationService.refund(idempotencyKey, req);
    }
}

