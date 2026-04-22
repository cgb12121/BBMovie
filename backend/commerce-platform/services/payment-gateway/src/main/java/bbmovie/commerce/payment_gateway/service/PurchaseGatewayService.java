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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PurchaseGatewayService {
    private static final String STATUS_KEY_PREFIX = "purchase:status:";

    private final PromotionClient promotionClient;
    private final PaymentOrchestratorClient orchestratorClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration statusTtl;

    public PurchaseGatewayService(PromotionClient promotionClient,
                                  PaymentOrchestratorClient orchestratorClient,
                                  StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${gateway.purchase-status.ttl-seconds:86400}") long statusTtlSeconds) {
        this.promotionClient = promotionClient;
        this.orchestratorClient = orchestratorClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.statusTtl = Duration.ofSeconds(statusTtlSeconds);
    }

    public PurchaseStartResponse startPurchase(String idempotencyKey, PurchaseStartRequest request) {
        PromotionResult promotion = promotionClient.tryApplyCoupon(
                new PromotionApplyRequest(request.couponCode(), request.userId(), request.amount())
        );
        BigDecimal discount = promotion.discountValue().setScale(2, RoundingMode.HALF_UP);
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
        saveStatus(gatewayRequestId, status);

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
        String payload = redisTemplate.opsForValue().get(statusKey(gatewayRequestId));
        if (payload == null || payload.isBlank()) {
            return new PurchaseStatusResponse(gatewayRequestId, null, "UNKNOWN_GATEWAY_REQUEST", Instant.now());
        }
        try {
            return objectMapper.readValue(payload, PurchaseStatusResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse purchase status from Redis", e);
        }
    }

    private void saveStatus(String gatewayRequestId, PurchaseStatusResponse status) {
        try {
            String payload = objectMapper.writeValueAsString(status);
            redisTemplate.opsForValue().set(statusKey(gatewayRequestId), payload, statusTtl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize purchase status", e);
        }
    }

    private String statusKey(String gatewayRequestId) {
        return STATUS_KEY_PREFIX + gatewayRequestId;
    }
}
