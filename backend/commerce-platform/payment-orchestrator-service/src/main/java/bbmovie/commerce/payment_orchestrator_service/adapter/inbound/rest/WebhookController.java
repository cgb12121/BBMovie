package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.helper.HttpHeaderExtractor;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.WebhookHandleResult;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.WebhookHandleStatus;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.WebhookUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookUseCase webhookOrchestrationService;

    @PostMapping("/{provider}")
    public ResponseEntity<String> handle(
            @PathVariable("provider") String provider,
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request
    ) {
        log.info("Webhook request: {}, {}, {}", provider, rawBody, request);
        WebhookHandleResult result = webhookOrchestrationService.handle(
            provider, 
            rawBody, 
            HttpHeaderExtractor.extractHeaders(request)
        );
        log.info(
                "Webhook request handled: provider={}, status={}, message={}",
                provider,
                result.status(),
                result.message()
        );

        if (result.status() == WebhookHandleStatus.INVALID_SIGNATURE) {
            return ResponseEntity.status(401).body(result.message());
        }
        
        return ResponseEntity.ok(result.message());
    }
}

