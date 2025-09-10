package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import com.bbmovie.payment.entity.enums.VoucherType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "vouchers")
@NoArgsConstructor
@AllArgsConstructor
public class Voucher extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private VoucherType type;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage; // for PERCENTAGE type, 0..100

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount; // for FIXED_AMOUNT type

    @Column(name = "user_specific_id")
    private String userSpecificId; // null = community, otherwise only this userId can redeem

    @Column(name = "permanent")
    private boolean permanent; // if true, ignore endAt

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt; // if permanent=false, this is the expiration

    @Builder.Default
    @Column(name = "max_use_per_user")
    private int maxUsePerUser = 1; // one usage per account by default

    @Builder.Default
    @Column(name = "active")
    private boolean active = true;
}

