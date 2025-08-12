package com.bbmovie.payment.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {
    private String provider;
    private String paymentId;
    private String reason;
    private java.math.BigDecimal amount; // optional partial refund amount (provider dependent)
}
