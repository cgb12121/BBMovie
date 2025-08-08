package com.bbmovie.payment.repository

import com.bbmovie.payment.entity.PaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentTransactionRepository : JpaRepository<PaymentTransaction?, UUID?> {
    fun findByPaymentGatewayId(paymentGatewayId: String?): Optional<PaymentTransaction?>?
    fun save(transaction: PaymentTransaction?)
}