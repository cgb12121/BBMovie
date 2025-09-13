package com.bbmovie.payment.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiscountCampaignResponse {
    private UUID id;
    private String name;
    private UUID planId;
    private BigDecimal discountPercent;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean active;
}


