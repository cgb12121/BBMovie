package com.bbmovie.payment.service.payment.zalopay

object ZaloPayConstraint {
    const val ONLY_SUPPORTED_CURRENCY = "VND"

    // Endpoints
    const val CREATE_ORDER_URL_SANDBOX = "https://sandbox.zalopay.com.vn/v001/tpe/createorder"
    const val CREATE_ORDER_URL_PROD = "https://zalopay.com.vn/v001/tpe/createorder"

    // Callback field names for ZaloPay webhook
    const val CALLBACK_DATA = "data"
    const val CALLBACK_MAC = "mac"

    // Vietnam timezone for apptransid prefix (yymmdd)
    const val VIETNAM_TZ = "Asia/Ho_Chi_Minh"
}


