package com.bbmovie.payment.dto;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentResponse {
    private String transactionId;
    private PaymentStatus status;
    private String providerReference;
}
