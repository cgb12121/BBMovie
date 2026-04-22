package com.bbmovie.referralservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "referral_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralUser extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "referral_code", nullable = false, unique = true)
    private String referralCode;
}
