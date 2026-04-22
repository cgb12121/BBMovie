package com.bbmovie.auth.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDto {
    private String provider;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String orderId;
}