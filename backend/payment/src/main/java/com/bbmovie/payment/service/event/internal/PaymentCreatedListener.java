package com.bbmovie.payment.service.event.internal;

import com.bbmovie.payment.dto.PaymentCreatedEvent;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentCreatedListener {

    private final StringRedisTemplate redisTemplate;

    //TODO: implement
    public PaymentCreatedListener(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @EventListener
    @Transactional
    public void handle(PaymentCreatedEvent event) {

        long ttlSeconds = Duration.between(Instant.now(), event.expiresAt()).toSeconds();
        redisTemplate.opsForValue().set(
                "transaction:" + event.orderId(),
                PaymentStatus.PENDING.getStatus(),
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }
}
