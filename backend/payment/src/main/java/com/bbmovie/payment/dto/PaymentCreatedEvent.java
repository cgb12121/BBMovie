package com.bbmovie.payment.dto;

import java.time.Instant;

public record PaymentCreatedEvent(String orderId, Instant expiresAt) {}
