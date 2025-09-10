package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "discount_campaigns")
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCampaign extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan; // campaign is per-plan as requested

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent; // percentage off 0..100

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Builder.Default
    @Column(name = "active")
    private boolean active = true;
}


