package com.bbmovie.payment.service.momo;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentNormalizer;
import org.springframework.stereotype.Component;

@Component("momoNormalizer")
public class MomoPaymentNormalizer implements PaymentNormalizer {
    @Override
    public PaymentStatus.NormalizedPaymentStatus normalize(Object providerStatus) {
        return switch (providerStatus.toString()) {
            case "0" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.SUCCEEDED, "Payment successful");
            case "1" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.FAILED, "Payment failed");
            case "4" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.CANCELLED, "Payment cancelled by user");
            default -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.FAILED, "Unknown MoMo status: " + providerStatus);
        };
    }
}
