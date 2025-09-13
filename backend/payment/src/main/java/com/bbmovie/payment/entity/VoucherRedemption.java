package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@Table(name = "voucher_redemptions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_voucher_user", columnNames = {"voucher_id", "user_id"})
})
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRedemption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;
}


