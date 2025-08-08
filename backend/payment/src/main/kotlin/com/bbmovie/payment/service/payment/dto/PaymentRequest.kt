package com.bbmovie.payment.service.payment.dto

import java.math.BigDecimal

data class PaymentRequest(
    var paymentMethodId: String? = null,
    var amount: BigDecimal? = null,
    var currency: String? = null,
    var userId: String? = null,
    var orderId: String? = null
)
