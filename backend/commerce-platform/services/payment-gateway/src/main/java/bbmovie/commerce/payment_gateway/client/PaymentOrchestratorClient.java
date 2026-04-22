package bbmovie.commerce.payment_gateway.client;

import bbmovie.commerce.payment_gateway.client.dto.OrchestratorCheckoutRequest;
import bbmovie.commerce.payment_gateway.client.dto.OrchestratorCheckoutResponse;
import bbmovie.commerce.payment_gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PaymentOrchestratorClient {
    private final RestClient.Builder restClientBuilder;
    private final GatewayProperties properties;

    public OrchestratorCheckoutResponse checkout(String idempotencyKey, OrchestratorCheckoutRequest request) {
        RestClient client = restClientBuilder.baseUrl(properties.getPaymentOrchestrator().getBaseUrl()).build();
        return client.post()
                .uri("/api/v1/payments/checkout")
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .body(OrchestratorCheckoutResponse.class);
    }
}
