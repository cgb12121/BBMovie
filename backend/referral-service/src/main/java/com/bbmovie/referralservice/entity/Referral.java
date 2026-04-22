package com.bbmovie.referralservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Entity
@Table(name = "referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Referral extends BaseEntity {

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "referred_id", nullable = false, unique = true)
    private UUID referredId;

    @Builder.Default
    @Column(name = "status")
    private String status = "JOINED"; // JOINED, REWARDED

    @Column(name = "reward_id")
    private String rewardId;

    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;
}
