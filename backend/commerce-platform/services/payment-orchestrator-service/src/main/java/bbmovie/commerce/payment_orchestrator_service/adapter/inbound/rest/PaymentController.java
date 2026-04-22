package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutResponse;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.CheckoutUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CheckoutUseCase checkoutOrchestrationService;

    @PostMapping("/checkout")
    public CheckoutResponse checkout(
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest req
    ) {
        return checkoutOrchestrationService.checkout(idempotencyKey, req);
    }
}

