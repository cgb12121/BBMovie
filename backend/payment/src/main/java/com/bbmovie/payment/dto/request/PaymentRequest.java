package com.bbmovie.payment.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentRequest {
    private String provider;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String orderId;
    private Integer expiresInMinutes; // optional TTL for order expiration
}