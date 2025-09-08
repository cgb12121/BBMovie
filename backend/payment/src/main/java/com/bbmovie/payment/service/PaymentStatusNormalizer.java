package com.bbmovie.payment.service;

import com.bbmovie.payment.entity.enums.PaymentStatus;

public interface PaymentStatusNormalizer {
    PaymentStatus.NormalizedPaymentStatus normalize(Object providerStatus);
}
