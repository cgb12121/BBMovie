package bbmovie.commerce.payment_gateway.client.dto;

public record OrchestratorCheckoutResponse(
        String orchestratorPaymentId,
        String provider,
        String providerPaymentId,
        String status,
        String paymentUrl,
        String clientSecret
) {
}
