package com.bbmovie.payment.service.payment.dto

import com.bbmovie.payment.entity.PaymentStatus

data class PaymentResponse(
    var transactionId: String? = null,
    var status: PaymentStatus? = null,
    var providerReference: String? = null
)
