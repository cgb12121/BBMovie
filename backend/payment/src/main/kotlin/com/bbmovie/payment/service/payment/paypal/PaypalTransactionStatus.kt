package com.bbmovie.payment.service.payment.paypal

enum class PaypalTransactionStatus(val status: String) {
    APPROVED("APPROVED"),
    COMPLETED("COMPLETED")
}
