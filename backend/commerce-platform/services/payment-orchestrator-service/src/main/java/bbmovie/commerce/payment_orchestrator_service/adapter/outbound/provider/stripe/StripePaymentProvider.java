package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.stripe;

import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.helper.ProviderAmountMapper;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.helper.ProviderStatusMapper;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.ProviderWebhookEvent;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.WebhookPayload;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.stripe.StripeProperties.StripeConstrains.HEADER_STRIPE_SIGNATURE;

@Component
@Slf4j
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentCreationPort, RefundPort, WebhookPort {

    private final StripeProperties stripeProperties;
    private final ObjectMapper objectMapper;

    @Override
    public ProviderType type() {
        return ProviderType.STRIPE;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(true, true, true, true, false);
    }

    @Override
    public PaymentResult createPayment(CreatePaymentCommand cmd) {
        requireSecretKey();
        Stripe.apiKey = stripeProperties.getSecretKey();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", ProviderAmountMapper.toStripeAmount(cmd.amount().amount()));
            params.put("currency", cmd.amount().currency().toLowerCase());
            params.put("description", cmd.purpose());
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", cmd.userId());
            metadata.put("purpose", cmd.purpose());
            if (cmd.metadata() != null) {
                metadata.putAll(cmd.metadata());
            }
            params.put("metadata", metadata);
            params.put("automatic_payment_methods", Map.of("enabled", true));

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return new PaymentResult(
                    OrchestratorPaymentId.newId(),
                    ProviderType.STRIPE,
                    new ProviderPaymentId(paymentIntent.getId()),
                    ProviderStatusMapper.map(
                            ProviderType.STRIPE,
                            ProviderStatusMapper.StatusSource.PROVIDER_STATUS,
                            paymentIntent.getStatus()
                    ),
                    null,
                    paymentIntent.getClientSecret(),
                    Map.of("providerStatus", paymentIntent.getStatus())
            );
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe create payment failed", e);
        }
    }

    @Override
    public PaymentResult refund(String orchestratorPaymentId, ProviderPaymentId providerPaymentId) {
        requireSecretKey();
        Stripe.apiKey = stripeProperties.getSecretKey();
        try {
            Refund refund = Refund.create(Map.of("payment_intent", providerPaymentId.value()));
            return new PaymentResult(
                    new OrchestratorPaymentId(UUID.fromString(orchestratorPaymentId)),
                    ProviderType.STRIPE,
                    providerPaymentId,
                    "succeeded".equalsIgnoreCase(refund.getStatus()) ? PaymentStatus.REFUNDED : PaymentStatus.FAILED,
                    null,
                    null,
                    Map.of("refundId", refund.getId(), "providerStatus", refund.getStatus())
            );
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe refund failed", e);
        }
    }

    @Override
    public boolean verifyWebhook(WebhookPayload payload) {
        String webhookSecret = stripeProperties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        String signature = payload.header(HEADER_STRIPE_SIGNATURE).orElse(null);
        if (signature == null || signature.isBlank() || payload.rawBody() == null) {
            return false;
        }
        try {
            Webhook.constructEvent(payload.rawBody(), signature, webhookSecret);
            return true;
        } catch (SignatureVerificationException e) {
            return false;
        }
    }

    @Override
    public ProviderWebhookEvent parseWebhook(WebhookPayload payload) {
        String signature = payload.header(HEADER_STRIPE_SIGNATURE).orElse(null);
        String webhookSecret = stripeProperties.getWebhookSecret();
        try {
            Event event = Webhook.constructEvent(payload.rawBody(), signature, webhookSecret);
            String eventType = event.getType();
            String paymentIntentId = extractPaymentIntentId(event, payload.rawBody());
            PaymentStatus status = ProviderStatusMapper.map(
                    ProviderType.STRIPE,
                    ProviderStatusMapper.StatusSource.WEBHOOK_EVENT_TYPE,
                    eventType
            );
            return new ProviderWebhookEvent(
                    ProviderType.STRIPE,
                    event.getId(),
                    new ProviderPaymentId(paymentIntentId),
                    status,
                    eventType
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Stripe webhook payload", e);
        }
    }

    private void requireSecretKey() {
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new IllegalStateException("Stripe secret key is missing");
        }
    }

    private String extractPaymentIntentId(Event event, String rawBody) {
        // For v1 we only support payment_intent and charge events.
        if (event.getDataObjectDeserializer().getObject().isPresent()) {
            Object obj = event.getDataObjectDeserializer().getObject().get();
            switch (obj) {
                case PaymentIntent intent: return intent.getId();
                case com.stripe.model.Charge charge: return charge.getPaymentIntent();
                default: throw new IllegalArgumentException("Unsupported Stripe event object for payment id extraction");
            }
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode objectNode = root.path("data").path("object");
            String objectType = objectNode.path("object").asText("");
            if ("payment_intent".equals(objectType)) {
                String paymentIntentId = objectNode.path("id").asText("");
                if (!paymentIntentId.isBlank()) {
                    return paymentIntentId;
                }
            }
            if ("charge".equals(objectType)) {
                String paymentIntentId = objectNode.path("payment_intent").asText("");
                if (!paymentIntentId.isBlank()) {
                    return paymentIntentId;
                }
            }
            String objectId = objectNode.path("id").asText("");
            if (!objectId.isBlank()) {
                log.info(
                        "Stripe webhook using object id fallback for non-payment event: eventId={}, objectType={}",
                        root.path("id").asText("unknown"),
                        objectType
                );
                return objectId;
            }
        } catch (Exception ignored) {
            // fall through to error below
        }
        throw new IllegalArgumentException("Unsupported Stripe event object for payment id extraction");
    }
}

