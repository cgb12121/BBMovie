package com.bbmovie.payment.service.event.internal;

import com.bbmovie.payment.dto.PaymentCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PaymentPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public PaymentPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishCreated(String orderId, Instant expiresAt) {
        eventPublisher.publishEvent(new PaymentCreatedEvent(orderId, expiresAt));
    }
}
