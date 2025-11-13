package com.bbmovie.payment.service.payment.provider.stripe;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentNormalizer;
import org.springframework.stereotype.Component;

@Component("stripeNormalizer")
public class StripePaymentNormalizer implements PaymentNormalizer {
    @Override
    public PaymentStatus.NormalizedPaymentStatus normalize(Object providerStatus) {
        return switch (providerStatus.toString()) {
            case "succeeded" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.SUCCEEDED, "Payment succeeded");
            case "requires_capture" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.PENDING, "Requires capture");
            case "processing" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.PENDING, "Payment is processing");
            case "requires_payment_method" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.FAILED, "Requires new payment method");
            case "canceled" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.CANCELLED, "Payment was canceled");
            default -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.FAILED, "Unknown Stripe status: " + providerStatus);
        };
    }
}
