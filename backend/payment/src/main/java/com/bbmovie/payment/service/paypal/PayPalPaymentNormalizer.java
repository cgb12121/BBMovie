package com.bbmovie.payment.service.paypal;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentNormalizer;
import org.springframework.stereotype.Component;

@Component("paypalNormalizer")
public class PayPalPaymentNormalizer implements PaymentNormalizer {
    @Override
    public PaymentStatus.NormalizedPaymentStatus normalize(Object providerStatus) {
        return switch (providerStatus.toString().toLowerCase()) {
            case "created" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.PENDING, "Payment created, awaiting approval");
            case "approved" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.SUCCEEDED, "Payment approved");
            case "failed" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.FAILED, "Payment failed");
            default -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.FAILED, "Unknown PayPal state: " + providerStatus);
        };
    }
}
