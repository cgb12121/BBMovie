package com.bbmovie.promotionservice.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CouponApplyRequest {
    private String code;
    private UUID userId;
    private Double cartValue;
}
