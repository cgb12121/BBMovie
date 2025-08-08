package com.bbmovie.payment.controller

import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.service.payment.dto.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/payment")
class PaymentController @Autowired constructor(private val paymentService: PaymentService) {

    @PostMapping("/initiate")
    fun initiatePayment(
        @RequestBody request: PaymentRequestDto,
        httpServletRequest: HttpServletRequest?
    ): ResponseEntity<ApiResponse<PaymentResponse?>?> {
        try {
            val paymentRequest: PaymentRequest = PaymentRequest()
            paymentRequest.amount = request.amount
            paymentRequest.currency = request.currency
            paymentRequest.userId = request.userId
            paymentRequest.orderId = request.orderId
            val response: PaymentResponse? = paymentService.processPayment(
                request.getProvider(), paymentRequest, httpServletRequest
            )
            return ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(PaymentResponse(null, PaymentStatus.FAILED, e.message).toString())
            )
        }
    }

    @GetMapping("/vnpay/callback")
    fun handleVNPayCallback(
        @RequestParam params: MutableMap<String?, String?>?,
        httpServletRequest: HttpServletRequest?
    ): ResponseEntity<ApiResponse<PaymentVerification?>?> {
        try {
            val verification: PaymentVerification? = paymentService.verifyPayment(
                "vnpayProvider", params, httpServletRequest
            )
            return ResponseEntity.ok(ApiResponse.success(verification))
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(PaymentVerification(false, null).toString())
            )
        }
    }

    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestHeader("X-Payment-Provider") provider: String?,
        @RequestBody paymentData: MutableMap<String?, String?>?,
        httpServletRequest: HttpServletRequest?
    ): ResponseEntity<ApiResponse<PaymentVerification?>?> {
        try {
            val verification: PaymentVerification? =
                paymentService.verifyPayment(provider, paymentData, httpServletRequest)
            return ResponseEntity.ok(ApiResponse.success(verification))
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(PaymentVerification(false, null).toString())
            )
        }
    }

    @PostMapping("/refund")
    fun refundPayment(
        @RequestBody request: RefundRequestDto,
        httpServletRequest: HttpServletRequest?
    ): ResponseEntity<ApiResponse<RefundResponse?>?> {
        try {
            val response: RefundResponse? = paymentService.refundPayment(
                request.getProvider(), request.getPaymentId(), httpServletRequest
            )
            return ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(RefundResponse(null, PaymentStatus.FAILED.getStatus).toString())
            )
        }
    }
}