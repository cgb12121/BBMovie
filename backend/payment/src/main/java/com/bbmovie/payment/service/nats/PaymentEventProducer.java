package com.bbmovie.payment.service.nats;

public interface PaymentEventProducer {
    <T> void publish(String subject, T event);

    void publishSubscriptionSuccessEvent();

    void publishSubscriptionCancelEvent();

    void publishSubscriptionRenewEvent();
}
