package com.bbmovie.payment.entity

import com.bbmovie.payment.entity.base.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_subscriptions")
open class UserSubscription : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    open lateinit var user: UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    open lateinit var plan: SubscriptionPlan

    @Column(name = "start_date", nullable = false)
    open lateinit var startDate: LocalDateTime

    @Column(name = "end_date", nullable = false)
    open lateinit var endDate: LocalDateTime

    @Column(name = "is_active")
    open var isActive: Boolean = true

    @Column(name = "auto_renew")
    open var autoRenew: Boolean = false

    @Column(name = "last_payment_date")
    open var lastPaymentDate: LocalDateTime? = null

    @Column(name = "next_payment_date")
    open var nextPaymentDate: LocalDateTime? = null

    @Column(name = "payment_provider")
    @Enumerated(EnumType.STRING)
    open var paymentProvider: PaymentProvider? = null

    @Column(name = "payment_gateway_id")
    open var paymentGatewayId: String? = null
}