package bbmovie.commerce.payment_gateway.dto;

import java.time.Instant;

public record PurchaseStatusResponse(
        String gatewayRequestId,
        String orchestratorPaymentId,
        String status,
        Instant updatedAt
) {
}
