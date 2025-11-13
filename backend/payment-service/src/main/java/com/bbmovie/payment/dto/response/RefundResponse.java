package com.bbmovie.payment.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private String refundId;
    private String status;
    private String message;
    private String currency;
    private BigDecimal amount;
}