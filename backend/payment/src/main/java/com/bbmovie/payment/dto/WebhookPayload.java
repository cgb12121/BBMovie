package com.bbmovie.payment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebhookPayload<T> {
    private String provider;
    private T rawData;
    private String signature;
}