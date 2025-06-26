package com.example.bbmovie.dto.request;

import lombok.Data;

@Data
public class RefundRequestDto {
    private String provider;
    private String paymentId;
}