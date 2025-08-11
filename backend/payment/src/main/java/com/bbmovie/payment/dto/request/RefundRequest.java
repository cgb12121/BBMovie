package com.bbmovie.payment.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {
    private String provider;
    private String paymentId;
}
