package com.bbmovie.payment.service.payment

import com.bbmovie.payment.entity.PaymentProvider
import com.bbmovie.payment.service.payment.dto.PaymentRequest
import com.bbmovie.payment.service.payment.dto.PaymentResponse
import com.bbmovie.payment.service.payment.dto.PaymentVerification
import com.bbmovie.payment.service.payment.dto.RefundResponse
import jakarta.servlet.http.HttpServletRequest

interface PaymentProviderAdapter {
    fun processPayment(request: PaymentRequest, httpServletRequest: HttpServletRequest): PaymentResponse

    fun verifyPayment(
        paymentData: MutableMap<String, String>,
        httpServletRequest: HttpServletRequest
    ): PaymentVerification

    fun refundPayment(paymentId: String, httpServletRequest: HttpServletRequest): RefundResponse

    fun getPaymentProviderName(): PaymentProvider
}