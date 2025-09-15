package com.bbmovie.payment.service;

import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.PaymentProvider;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;

public interface PaymentRecordService {
    PaymentTransaction createPendingTransaction(
            String userId, SubscriptionPlan plan, BigDecimal amount, CurrencyUnit currency,
            PaymentProvider provider, String providerTransactionId, String description
    );
}
