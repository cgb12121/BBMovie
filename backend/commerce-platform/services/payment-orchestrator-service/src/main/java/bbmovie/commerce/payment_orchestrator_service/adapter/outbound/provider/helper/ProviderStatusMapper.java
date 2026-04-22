package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.helper;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;

public final class ProviderStatusMapper {

    private ProviderStatusMapper() {
    }

    public enum StatusSource {
        PROVIDER_STATUS,
        WEBHOOK_EVENT_TYPE
    }

    public static PaymentStatus map(ProviderType providerType, StatusSource source, String rawValue) {
        return switch (providerType) {
            case STRIPE -> mapStripe(source, rawValue);
            case PAYPAL -> mapPaypal(source, rawValue);
            default -> PaymentStatus.PENDING;
        };
    }

    private static PaymentStatus mapStripe(StatusSource source, String rawValue) {
        if (source == StatusSource.PROVIDER_STATUS) {
            if (rawValue == null) {
                return PaymentStatus.PENDING;
            }
            return switch (rawValue) {
                case "succeeded" -> PaymentStatus.SUCCEEDED;
                case "canceled" -> PaymentStatus.CANCELLED;
                case "requires_payment_method", "requires_confirmation", "requires_action", "processing", "requires_capture" ->
                        PaymentStatus.PENDING;
                default -> PaymentStatus.FAILED;
            };
        }
        return switch (rawValue) {
            case "payment_intent.succeeded" -> PaymentStatus.SUCCEEDED;
            case "payment_intent.payment_failed" -> PaymentStatus.FAILED;
            case "charge.refunded" -> PaymentStatus.REFUNDED;
            case "payment_intent.canceled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PENDING;
        };
    }

    private static PaymentStatus mapPaypal(StatusSource source, String rawValue) {
        if (source == StatusSource.PROVIDER_STATUS) {
            if (rawValue == null) {
                return PaymentStatus.PENDING;
            }
            return switch (rawValue.toUpperCase()) {
                case "COMPLETED" -> PaymentStatus.SUCCEEDED;
                case "VOIDED", "CANCELLED" -> PaymentStatus.CANCELLED;
                case "CREATED", "SAVED", "APPROVED", "PAYER_ACTION_REQUIRED" -> PaymentStatus.PENDING;
                default -> PaymentStatus.FAILED;
            };
        }
        return switch (rawValue) {
            case "PAYMENT.CAPTURE.COMPLETED", "CHECKOUT.ORDER.APPROVED", "CHECKOUT.ORDER.COMPLETED" ->
                    PaymentStatus.SUCCEEDED;
            case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.DECLINED" -> PaymentStatus.FAILED;
            case "PAYMENT.CAPTURE.REFUNDED" -> PaymentStatus.REFUNDED;
            case "CHECKOUT.ORDER.VOIDED" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PENDING;
        };
    }
}

