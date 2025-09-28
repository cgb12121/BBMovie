package com.bbmovie.payment.service.cache;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RedisKeyExpirationListener implements MessageListener {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public RedisKeyExpirationListener(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString(); // e.g. "transaction:12345"
        if (expiredKey.startsWith("transaction:")) {
            String orderId = expiredKey.split(":")[1];
            paymentTransactionRepository.findByProviderTransactionId(orderId).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.AUTO_CANCELLED);
                    paymentTransactionRepository.save(payment);
                }
            });
        }
    }
}
