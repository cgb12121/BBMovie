package com.example.bbmovie.service.payment.dto;

import com.example.bbmovie.service.payment.PaymentProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
public class PaymentRequest {
    private BigDecimal amount;
    private String currency;
    private PaymentProviderType provider;
    private String description;
    private String ipAddress;
}
