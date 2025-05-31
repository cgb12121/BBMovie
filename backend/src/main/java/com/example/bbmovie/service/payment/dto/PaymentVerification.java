package com.example.bbmovie.service.payment.dto;

import lombok.*;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerification {
    private boolean isValid;
    private String transactionId;
}