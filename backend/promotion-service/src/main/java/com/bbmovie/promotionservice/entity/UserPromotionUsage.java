package com.bbmovie.promotionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_promotion_usage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPromotionUsage extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "coupon_code")
    private String couponCode; // Optional, if used via coupon
}
