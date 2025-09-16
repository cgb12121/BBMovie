package com.bbmovie.payment.service.event.internal;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class RedisKeyExpirationListener implements MessageListener {

    private final PaymentTransactionRepository repository;

    @Autowired
    public RedisKeyExpirationListener(PaymentTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString(); // e.g. "transaction:12345"
        if (expiredKey.startsWith("transaction:")) {
            String orderId = expiredKey.split(":")[1];
            repository.findById(UUID.fromString(orderId)).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.AUTO_CANCELLED);
                    repository.save(payment);
                }
            });
        }
    }
}
