package com.bbmovie.payment.service;

import com.bbmovie.payment.entity.enums.PaymentStatus;

public interface PaymentNormalizer {
    PaymentStatus.NormalizedPaymentStatus normalize(Object providerStatus);
}