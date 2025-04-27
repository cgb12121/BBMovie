package com.example.bbmovie.entity;

import com.example.bbmovie.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@ToString
public class SubscriptionPlan extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "price_per_month", nullable = false)
    private BigDecimal pricePerMonth;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "max_simultaneous_streams")
    private Integer maxSimultaneousStreams;

    @Column(name = "video_quality")
    @Enumerated(EnumType.STRING)
    private VideoQuality videoQuality;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "features", length = 2000)
    private String features; // JSON string of features

    public enum VideoQuality {
        SD, HD, FHD, UHD
    }
} 