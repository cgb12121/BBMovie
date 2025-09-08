package com.bbmovie.payment.service.zalopay;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentStatusNormalizer;
import org.springframework.stereotype.Component;

@Component("zalopayNormalizer")
public class ZaloPayPaymentStatusNormalizer implements PaymentStatusNormalizer {
    @Override
    public PaymentStatus.NormalizedPaymentStatus normalize(Object providerStatus) {
        return switch (providerStatus.toString()) {
            case "1" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.PENDING, "Payment in progress, please retry status query");
            case "2" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.SUCCEEDED, "Payment successful");
            case "SPENDING" -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.PENDING, "Payment is pending settlement");
            default -> new PaymentStatus.NormalizedPaymentStatus(PaymentStatus.FAILED, "Unknown ZaloPay status: " + providerStatus);
        };
    }
}