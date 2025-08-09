package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "user_subscriptions")
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscription extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "auto_renew")
    private boolean autoRenew = false;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Column(name = "next_payment_date")
    private LocalDateTime nextPaymentDate;

    @Column(name = "payment_provider")
    @Enumerated(EnumType.STRING)
    private PaymentProvider paymentProvider;

    @Column(name = "payment_gateway_id")
    private String paymentGatewayId;
}
