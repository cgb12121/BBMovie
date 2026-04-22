package bbmovie.commerce.payment_gateway.service;

import bbmovie.commerce.payment_gateway.client.PaymentOrchestratorClient;
import bbmovie.commerce.payment_gateway.client.PromotionClient;
import bbmovie.commerce.payment_gateway.client.dto.OrchestratorCheckoutRequest;
import bbmovie.commerce.payment_gateway.client.dto.OrchestratorCheckoutResponse;
import bbmovie.commerce.payment_gateway.client.dto.PromotionApplyRequest;
import bbmovie.commerce.payment_gateway.client.dto.PromotionResult;
import bbmovie.commerce.payment_gateway.dto.PurchaseStartRequest;
import bbmovie.commerce.payment_gateway.dto.PurchaseStartResponse;
import bbmovie.commerce.payment_gateway.dto.PurchaseStatusResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PurchaseGatewayService {
    private final PromotionClient promotionClient;
    private final PaymentOrchestratorClient orchestratorClient;
    private final Map<String, PurchaseStatusResponse> statusStore = new ConcurrentHashMap<>();

    public PurchaseGatewayService(PromotionClient promotionClient, PaymentOrchestratorClient orchestratorClient) {
        this.promotionClient = promotionClient;
        this.orchestratorClient = orchestratorClient;
    }

    public PurchaseStartResponse startPurchase(String idempotencyKey, PurchaseStartRequest request) {
        PromotionResult promotion = promotionClient.tryApplyCoupon(
                new PromotionApplyRequest(request.couponCode(), request.userId(), request.amount().doubleValue())
        );
        BigDecimal discount = BigDecimal.valueOf(promotion.discountValue()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = request.amount().subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        Map<String, String> metadata = new HashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        metadata.put("gatewayOriginalAmount", request.amount().toPlainString());
        metadata.put("gatewayFinalAmount", finalAmount.toPlainString());
        metadata.put("promotionApplied", String.valueOf(promotion.applied()));
        if (promotion.promotionId() != null) {
            metadata.put("promotionId", promotion.promotionId());
        }
        if (request.couponCode() != null) {
            metadata.put("couponCode", request.couponCode());
        }

        OrchestratorCheckoutResponse checkout = orchestratorClient.checkout(
                idempotencyKey,
                new OrchestratorCheckoutRequest(
                        request.userId(),
                        request.userEmail(),
                        request.provider(),
                        finalAmount,
                        request.currency(),
                        request.purpose(),
                        metadata
                )
        );

        String gatewayRequestId = UUID.randomUUID().toString();
        PurchaseStatusResponse status = new PurchaseStatusResponse(
                gatewayRequestId,
                checkout.orchestratorPaymentId(),
                checkout.status(),
                Instant.now()
        );
        statusStore.put(gatewayRequestId, status);

        return new PurchaseStartResponse(
                gatewayRequestId,
                checkout.orchestratorPaymentId(),
                checkout.providerPaymentId(),
                checkout.status(),
                checkout.paymentUrl(),
                checkout.clientSecret(),
                request.amount(),
                discount,
                finalAmount,
                promotion.message()
        );
    }

    public PurchaseStatusResponse getStatus(String gatewayRequestId) {
        return statusStore.getOrDefault(
                gatewayRequestId,
                new PurchaseStatusResponse(gatewayRequestId, null, "UNKNOWN_GATEWAY_REQUEST", Instant.now())
        );
    }
}
