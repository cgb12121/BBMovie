package com.bbmovie.payment.service.i18n;

import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.I18nService;
import com.bbmovie.payment.service.PaymentNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentI18nService {
     
    private final I18nService i18n;
    private final Map<String, PaymentNormalizer> paymentNormalizers;

    @Autowired
    public PaymentI18nService(I18nService i18n, Map<String, PaymentNormalizer> paymentNormalizers) {
        this.i18n = i18n;
        this.paymentNormalizers = paymentNormalizers;
    }

    public String messageFor(PaymentProvider provider, String providerStatusCode) {
        String beanName = provider.getName() + "Normalizer"; // e.g., paypalNormalizer, stripeNormalizer
        PaymentNormalizer normalizer = paymentNormalizers.get(beanName);
        PaymentStatus status;
        if (normalizer != null) {
            PaymentStatus.NormalizedPaymentStatus normalized = normalizer.normalize(providerStatusCode);
            status = normalized.status();
        } else {
            status = PaymentStatus.FAILED;
        }
        String statusKey = switch (status) {
            case SUCCEEDED -> "completed";
            case PENDING -> "pending";
            case FAILED, CANCELLED, AUTO_CANCELLED, REFUNDED -> "failed";
        };
        String key = "payment." + provider.getName() + "." + statusKey;
        return i18n.getMessage(key);
    }
}