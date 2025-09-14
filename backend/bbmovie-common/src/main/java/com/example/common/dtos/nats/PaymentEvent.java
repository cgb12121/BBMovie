package com.example.common.dtos.nats;

public record PaymentEvent(String userEmail, double amount) {}
