package com.example.bbmovie.service.payment.dto;

import com.example.bbmovie.entity.enumerate.PaymentStatus;
import lombok.*;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private String transactionId;
    private PaymentStatus status;
    private String providerReference;
}