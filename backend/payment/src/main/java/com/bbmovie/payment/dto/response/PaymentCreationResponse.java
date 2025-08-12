package com.bbmovie.payment.dto.response;

import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentCreationResponse {
    private PaymentProvider provider;
    private String providerTransactionId;
    private String serverTransactionId;
    private PaymentStatus serverStatus;
    private String providerPaymentLink;
}
