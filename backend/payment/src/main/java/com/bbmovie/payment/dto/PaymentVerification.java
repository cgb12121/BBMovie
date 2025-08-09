package com.bbmovie.payment.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerification {
    private boolean success;
    private String transactionId;
}
