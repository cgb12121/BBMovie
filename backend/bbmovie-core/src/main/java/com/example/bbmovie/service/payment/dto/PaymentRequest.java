package com.example.bbmovie.service.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String orderId;
}