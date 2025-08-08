package com.bbmovie.payment.service.payment.dto

data class RefundRequestDto(
    var provider: String? = null,
    var paymentId: String? = null
)
