package com.bbmovie.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {
    private String provider;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String orderId;
}
