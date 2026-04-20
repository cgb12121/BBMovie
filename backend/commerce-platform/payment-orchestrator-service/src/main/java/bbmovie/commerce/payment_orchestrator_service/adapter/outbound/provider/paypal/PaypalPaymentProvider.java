package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.paypal;

import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.helper.ProviderAmountMapper;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.helper.ProviderStatusMapper;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.ProviderWebhookEvent;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.WebhookPayload;
import bbmovie.commerce.payment_orchestrator_service.application.command.CreatePaymentCommand;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.PaymentCreationPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.ProviderCapabilities;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.RefundPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.WebhookPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;
import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpRequest;
import com.paypal.http.HttpResponse;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.LinkDescription;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.PurchaseUnitRequest;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.payments.CapturesRefundRequest;
import com.paypal.payments.Refund;
import com.paypal.payments.RefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.paypal.PaypalProperties.PaypalConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaypalPaymentProvider implements PaymentCreationPort, RefundPort, WebhookPort {

    private final PaypalProperties paypalProperties;
    private final ObjectMapper objectMapper;

    @Override
    public ProviderType type() {
        return ProviderType.PAYPAL;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(true, true, true, true, true);
    }

    @Override
    public PaymentResult createPayment(CreatePaymentCommand cmd) {
        PayPalHttpClient client = buildClient();
        try {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent(CAPTURE_INTENT);

            PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                    .referenceId(cmd.userId())
                    .description(cmd.purpose())
                    .amountWithBreakdown(new AmountWithBreakdown()
                            .currencyCode(cmd.amount().currency())
                            .value(ProviderAmountMapper.toPaypalAmount(cmd.amount().amount())));
            orderRequest.purchaseUnits(List.of(purchaseUnit));

            ApplicationContext applicationContext = new ApplicationContext()
                    .shippingPreference(SHIPPING_PREFERENCE)
                    .userAction(USER_ACTION_PAY_NOW);
            orderRequest.applicationContext(applicationContext);

            OrdersCreateRequest request = new OrdersCreateRequest();
            request.prefer(PREFER_RETURN_REPRESENTATION);
            request.requestBody(orderRequest);

            HttpResponse<Order> response = client.execute(request);
            Order order = response.result();
            String orderId = Objects.requireNonNull(order.id(), "PayPal order id is missing");
            String approveUrl = extractApproveUrl(order.links());
            String providerStatus = order.status();
            return new PaymentResult(
                    OrchestratorPaymentId.newId(),
                    ProviderType.PAYPAL,
                    new ProviderPaymentId(orderId),
                    ProviderStatusMapper.map(
                            ProviderType.PAYPAL,
                            ProviderStatusMapper.StatusSource.PROVIDER_STATUS,
                            providerStatus
                    ),
                    approveUrl,
                    null,
                    Map.of("providerStatus", providerStatus == null ? "CREATED" : providerStatus)
            );
        } catch (Exception e) {
            log.error("PayPal create payment failed", e);
            throw new IllegalStateException("PayPal create payment failed", e);
        }
    }

    @Override
    public PaymentResult refund(String orchestratorPaymentId, ProviderPaymentId providerPaymentId) {
        PayPalHttpClient client = buildClient();
        try {
            String targetCaptureId = providerPaymentId.value();
            Refund refund;
            try {
                refund = refundCapture(client, targetCaptureId);
            } catch (HttpException ex) {
                // Common integration case: caller passes PayPal order id instead of capture id.
                if (isInvalidResourceId(ex)) {
                    String resolvedCaptureId = resolveCaptureIdFromOrderId(client, providerPaymentId.value());
                    if (resolvedCaptureId == null || resolvedCaptureId.isBlank()) {
                        resolvedCaptureId = captureOrderAndResolveCaptureId(client, providerPaymentId.value());
                    }
                    if (resolvedCaptureId != null && !resolvedCaptureId.isBlank()) {
                        log.info(
                                "Resolved PayPal capture id from order id for refund: orderId={}, captureId={}",
                                providerPaymentId.value(),
                                resolvedCaptureId
                        );
                        targetCaptureId = resolvedCaptureId;
                        refund = refundCapture(client, targetCaptureId);
                    } else {
                        throw ex;
                    }
                } else {
                    throw ex;
                }
            }
            String status = refund.status();
            PaymentStatus normalized = PAYPAL_STATUS_COMPLETED.equalsIgnoreCase(status) ? PaymentStatus.REFUNDED : PaymentStatus.FAILED;
            
            return new PaymentResult(
                    new OrchestratorPaymentId(UUID.fromString(orchestratorPaymentId)),
                    ProviderType.PAYPAL,
                    new ProviderPaymentId(targetCaptureId),
                    normalized,
                    null,
                    null,
                    Map.of(
                            "refundId", refund.id() == null ? "" : refund.id(),
                            "providerStatus", status == null ? "UNKNOWN" : status
                    )
            );
        } catch (Exception e) {
            log.error("PayPal refund failed", e);
            throw new IllegalStateException("PayPal refund failed", e);
        }
    }

    private Refund refundCapture(PayPalHttpClient client, String captureId) throws Exception {
        RefundRequest refundRequest = new RefundRequest();
        CapturesRefundRequest request = new CapturesRefundRequest(captureId);
        request.requestBody(refundRequest);
        HttpResponse<Refund> response = client.execute(request);
        return response.result();
    }

    private boolean isInvalidResourceId(HttpException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("INVALID_RESOURCE_ID");
    }

    private String captureOrderAndResolveCaptureId(PayPalHttpClient client, String orderId) {
        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
            request.requestBody(new OrderRequest());
            request.prefer(PREFER_RETURN_REPRESENTATION);
            HttpResponse<Order> response = client.execute(request);
            Order capturedOrder = response.result();
            String captureId = extractCaptureId(capturedOrder);
            if (captureId != null && !captureId.isBlank()) {
                log.info("PayPal order captured for refund flow: orderId={}, captureId={}", orderId, captureId);
                return captureId;
            }
        } catch (Exception e) {
            log.warn("Failed to capture PayPal order before refund: {}", orderId, e);
        }
        return null;
    }

    private String resolveCaptureIdFromOrderId(PayPalHttpClient client, String orderId) {
        try {
            HttpRequest<String> request = new HttpRequest<>(
                    "/v2/checkout/orders/" + orderId,
                    HttpMethod.GET.name(),
                    String.class
            );
            request.header("Content-Type", "application/json");
            HttpResponse<String> response = client.execute(request);
            JsonNode root = objectMapper.readTree(response.result());
            return extractCaptureId(root);
        } catch (Exception e) {
            log.warn("Failed to resolve PayPal capture id from order id: {}", orderId, e);
        }
        return null;
    }

    private String extractCaptureId(Order order) {
        if (order == null || order.purchaseUnits() == null || order.purchaseUnits().isEmpty()) {
            return null;
        }
        var firstPurchaseUnit = order.purchaseUnits().get(0);
        if (firstPurchaseUnit == null
                || firstPurchaseUnit.payments() == null
                || firstPurchaseUnit.payments().captures() == null
                || firstPurchaseUnit.payments().captures().isEmpty()) {
            return null;
        }
        return firstPurchaseUnit.payments().captures().get(0).id();
    }

    private String extractCaptureId(JsonNode orderNode) {
        JsonNode captures = orderNode.path("purchase_units")
                .path(0)
                .path("payments")
                .path("captures");
        if (captures.isArray() && !captures.isEmpty()) {
            String captureId = captures.path(0).path("id").asText("");
            if (!captureId.isBlank()) {
                return captureId;
            }
        }
        return null;
    }

    @Override
    public boolean verifyWebhook(WebhookPayload payload) {
        String webhookId = paypalProperties.getWebhookId();
        if (webhookId == null || webhookId.isBlank()) {
            return false;
        }
        try {
            PayPalHttpClient client = buildClient();
            HttpRequest<String> request = new HttpRequest<>(
                    "/v1/notifications/verify-webhook-signature",
                    HttpMethod.POST.name(),
                    String.class
            );
            request.header("Content-Type", "application/json");
            request.requestBody(Map.of(
                    "transmission_id", payload.header(TRANSMISSION_ID_HEADER).orElse(""),
                    "transmission_time", payload.header(TRANSMISSION_TIME_HEADER).orElse(""),
                    "cert_url", payload.header(CERT_URL_HEADER).orElse(""),
                    "auth_algo", payload.header(AUTH_ALGO_HEADER).orElse(""),
                    "transmission_sig", payload.header(TRANSMISSION_SIG_HEADER).orElse(""),
                    "webhook_id", webhookId,
                    "webhook_event", objectMapper.readValue(
                        payload.rawBody(), 
                        new TypeReference<Map<String, Object>>() { }
                    )
            ));
            HttpResponse<String> response = client.execute(request);
            JsonNode verifyResponse = objectMapper.readTree(response.result());
            return VERIFICATION_STATUS_SUCCESS.equalsIgnoreCase(verifyResponse.path("verification_status").asText());
        } catch (Exception e) {
            log.error("PayPal verify webhook failed", e);
            return false;
        }
    }

    @Override
    public ProviderWebhookEvent parseWebhook(WebhookPayload payload) {
        try {
            JsonNode event = objectMapper.readTree(payload.rawBody());
            String eventId = event.path("id").asText("evt_paypal");
            String eventType = event.path("event_type").asText("CHECKOUT.ORDER.APPROVED");
            JsonNode resource = event.path("resource");
            String paymentId = resource.path("supplementary_data").path("related_ids").path("order_id").asText();
            if (paymentId == null || paymentId.isBlank()) {
                paymentId = resource.path("id").asText("pp_unknown");
            }
            return new ProviderWebhookEvent(
                    ProviderType.PAYPAL,
                    eventId,
                    new ProviderPaymentId(paymentId),
                    ProviderStatusMapper.map(
                            ProviderType.PAYPAL,
                            ProviderStatusMapper.StatusSource.WEBHOOK_EVENT_TYPE,
                            eventType
                    ),
                    eventType
            );
        } catch (Exception e) {
            log.error("PayPal parse webhook failed", e);
            throw new IllegalArgumentException("Invalid PayPal webhook payload", e);
        }
    }

    private PayPalHttpClient buildClient() {
        if (!paypalProperties.isConfigured()) {
            throw new IllegalStateException("PayPal credentials are not configured");
        }
        String baseUrl = paypalProperties.getBaseUrl();
        if (baseUrl != null && baseUrl.contains("sandbox")) {
            return new PayPalHttpClient(
                    new PayPalEnvironment.Sandbox(
                            paypalProperties.getClientId(),
                            paypalProperties.getClientSecret()
                    )
            );
        }
        return new PayPalHttpClient(
                new PayPalEnvironment.Live(
                        paypalProperties.getClientId(),
                        paypalProperties.getClientSecret()
                )
        );
    }

    private String extractApproveUrl(List<LinkDescription> links) {
        if (links == null) {
            return null;
        }
        for (LinkDescription link : links) {
            if (link != null && APPROVE_REL.equalsIgnoreCase(link.rel())) {
                return link.href();
            }
        }
        return null;
    }

}

