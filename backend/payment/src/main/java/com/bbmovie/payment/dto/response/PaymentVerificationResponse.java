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
    private Object clientResponse; // Use for FE
    private Object providerResponse; //To response provider Post Backend-to-backend callback
}