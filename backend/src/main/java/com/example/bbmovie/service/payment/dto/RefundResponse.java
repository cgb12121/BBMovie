package com.example.bbmovie.service.payment.dto;

import lombok.*;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundResponse {
    private String refundId;
    private String status;
}