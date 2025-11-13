package com.bbmovie.common.dtos.nats;

public record PaymentEvent(String userEmail, double amount) {}
