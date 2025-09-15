package com.bbmovie.payment.service.impl;

import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.UserSubscription;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public PaymentRecordServiceImpl(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    public PaymentTransaction createPendingTransaction(
            String userId, SubscriptionPlan plan, BigDecimal amount, CurrencyUnit currency,
            PaymentProvider provider, String providerTransactionId, String description
    ) {
        UserSubscription subscription = UserSubscription.builder()
                .userId(userId)
                .plan(plan)
                .build();

        PaymentTransaction txn = PaymentTransaction.builder()
                .userId(userId)
                .subscription(subscription)
                .baseAmount(amount)
                .currency(currency)
                .paymentProvider(provider)
                .providerTransactionId(providerTransactionId)
                .transactionDate(LocalDateTime.now())
                .status(PaymentStatus.PENDING)
                .description(description)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        return paymentTransactionRepository.save(txn);
    }
}