package com.bbmovie.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentRequest {
    private String paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String orderId;
}
