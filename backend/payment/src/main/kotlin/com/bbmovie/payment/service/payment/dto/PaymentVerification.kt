package com.bbmovie.payment.service.payment.dto

data class PaymentVerification(
    var success: Boolean = false,
    var transactionId: String? = null
)
