package com.bbmovie.payment.service.nats;

public interface PaymentEventProducer {
    <T> void publish(String subject, T event);

    <T> void publishSubscriptionSuccessEvent(T data);

    <T> void publishSubscriptionCancelEvent(T data);

    <T> void publishSubscriptionRenewEvent(T data);
}
