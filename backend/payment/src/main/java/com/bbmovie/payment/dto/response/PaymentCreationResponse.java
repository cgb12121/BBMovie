package com.bbmovie.payment.dto.response;

import com.bbmovie.payment.entity.enums.PaymentStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentCreationResponse {
    private String transactionId;
    private PaymentStatus status;
    private String providerReference;
}
