package com.bbmovie.payment.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerification {
    private boolean isValid;
    private String transactionId;
    private String code;
    private String message;
}
