package com.bbmovie.payment.service.payment.vnpay

object VnPayConstraint {
    const val VERSION = "2.1.0"
    const val DATE_FORMAT = "yyyyMMddHHmmss"
    const val ONLY_SUPPORTED_CURRENCY = "VND"
    const val RETURN_URL = "http://localhost:8080/api/payment/vnpay/return"
    const val PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
    const val TRANSACTION_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"
}
