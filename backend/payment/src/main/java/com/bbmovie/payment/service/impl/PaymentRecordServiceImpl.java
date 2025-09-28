package com.bbmovie.payment.service.impl;

import com.bbmovie.payment.dto.PaymentCreatedEvent;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.UserSubscription;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentRecordServiceImpl implements PaymentRecordService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public PaymentRecordServiceImpl(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    public PaymentTransaction createPendingTransaction(PaymentCreatedEvent event) {
        UserSubscription subscription = UserSubscription.builder()
                .userId(event.userId())
                .plan(event.plan())
                .build();

        PaymentTransaction txn = PaymentTransaction.builder()
                .userId(event.userId())
                .subscription(subscription)
                .baseAmount(event.amount())
                .currency(event.currency())
                .paymentProvider(event.provider())
                .providerTransactionId(event.providerTransactionId())
                .transactionDate(LocalDateTime.now())
                .status(PaymentStatus.PENDING)
                .description(event.description())
                .expiresAt(event.expiresAt())
                .build();
        return paymentTransactionRepository.save(txn);
    }
}