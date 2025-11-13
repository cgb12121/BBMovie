package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.PaymentCreatedEvent;
import com.bbmovie.payment.entity.PaymentTransaction;

public interface PaymentRecordService {
    PaymentTransaction createPendingTransaction(PaymentCreatedEvent event);
}
