package com.bbmovie.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationResponse {
    private boolean isValid;
    private String transactionId;
    private String code;
    private String message;
    private Object providerPayloadStringJson; // Use for FE
    private Object responseToProviderStringJson; //To response provider Post Backend-to-backend callback (Zalopay)
}
