package com.bbmovie.payment.service.payment.vnpay

enum class VnPayCommand(val command: String) {
    QUERY_DR("querydr"),
    REFUND("refund"),
    PAY("pay")
}
