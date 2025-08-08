package com.bbmovie.payment.entity

import com.bbmovie.payment.entity.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payment_transactions")
open class PaymentTransaction(
    user: String?,
    amount: BigDecimal?,
    currency: String?,
    paymentProvider: PaymentProvider,
    paymentMethod: String?,
    transactionDate: LocalDateTime,
    description: String
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    open lateinit var userId: UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    open var subscription: UserSubscription? = null

    @Column(name = "amount", nullable = false)
    open lateinit var amount: BigDecimal

    @Column(name = "currency", nullable = false)
    open lateinit var currency: String

    @Column(name = "payment_Provider", nullable = false)
    @Enumerated(EnumType.STRING)
    open lateinit var paymentProvider: PaymentProvider

    @Column(name = "payment_method", nullable = false)
    open lateinit var paymentMethod: String

    @Column(name = "payment_gateway_id", nullable = false)
    open lateinit var paymentGatewayId: String

    @Column(name = "payment_gateway_order_id")
    open var paymentGatewayOrderId: String? = null

    @Column(name = "provider_status", nullable = false)
    open lateinit var providerStatus: String

    @Column(name = "transaction_date", nullable = false)
    open lateinit var transactionDate: LocalDateTime

    @Column(name = "cancel_date")
    open var cancelDate: LocalDateTime? = null

    @Column(name = "refund_date")
    open var refundDate: LocalDateTime? = null

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    open lateinit var status: PaymentStatus

    @Column(name = "error_code")
    open var errorCode: String? = null

    @Column(name = "error_message")
    open var errorMessage: String? = null

    @Column(name = "ipn_url")
    open var ipnUrl: String? = null

    @Column(name = "return_url")
    open var returnUrl: String? = null

    @Column(name = "description")
    open var description: String? = null

    @Column(name = "payment_details", columnDefinition = "TEXT")
    open var paymentDetails: String? = null
}
