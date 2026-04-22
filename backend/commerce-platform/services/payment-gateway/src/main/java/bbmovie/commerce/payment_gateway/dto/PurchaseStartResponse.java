package bbmovie.commerce.payment_gateway.dto;

import java.math.BigDecimal;

public record PurchaseStartResponse(
        String gatewayRequestId,
        String orchestratorPaymentId,
        String providerPaymentId,
        String status,
        String paymentUrl,
        String clientSecret,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String promotionReason
) {
}
