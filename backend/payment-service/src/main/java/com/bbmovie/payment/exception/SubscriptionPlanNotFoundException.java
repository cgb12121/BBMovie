package com.bbmovie.payment.exception;

public class SubscriptionPlanNotFoundException extends RuntimeException {
    public SubscriptionPlanNotFoundException() {
        super("Subscription plan not found");
    }
}
