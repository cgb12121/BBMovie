package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest;

import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.OutboxStatusQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/outbox")
public class OutboxAdminController {

    private final OutboxStatusQueryService outboxStatusQueryService;

    @GetMapping("/status")
    public OutboxStatusQueryService.OutboxStatus status() {
        return outboxStatusQueryService.snapshot();
    }
}
