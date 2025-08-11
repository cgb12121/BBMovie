package com.bbmovie.payment.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private String refundId;
    private String status;
}
