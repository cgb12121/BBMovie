package com.bbmovie.promotionservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "current_usage")
    @Builder.Default
    private Integer currentUsage = 0;

    @Column(name = "min_purchase_amount")
    private Double minPurchaseAmount;
}
