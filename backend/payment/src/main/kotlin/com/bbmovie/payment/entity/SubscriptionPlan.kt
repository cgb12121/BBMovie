package com.bbmovie.payment.entity

import com.bbmovie.payment.entity.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "subscription_plans")
open class SubscriptionPlan : BaseEntity() {

    @Column(name = "name", nullable = false)
    open lateinit var name: String

    @Column(name = "base_amount", nullable = false)
    open lateinit var baseAmount: BigDecimal

    @Column(name = "base_currency", nullable = false)
    open lateinit var baseCurrency: String

    @Column(name = "billing_cycle", nullable = false)
    open lateinit var billingCycle: String

    @Column(name = "description")
    open var description: String? = null

    @Column(name = "features")
    open var features: String? = null

    @Column(name = "active", nullable = false)
    open var active: Boolean = true
}