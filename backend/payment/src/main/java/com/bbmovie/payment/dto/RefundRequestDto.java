package com.bbmovie.payment.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequestDto {
    private String provider;
    private String paymentId;
}
