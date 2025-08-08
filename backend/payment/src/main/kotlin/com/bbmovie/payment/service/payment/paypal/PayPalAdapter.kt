package com.bbmovie.payment.service.payment.paypal

import com.bbmovie.payment.entity.PaymentProvider
import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.exception.PayPalPaymentException
import com.bbmovie.payment.service.payment.PaymentProviderAdapter
import com.bbmovie.payment.service.payment.dto.PaymentRequest
import com.bbmovie.payment.service.payment.dto.PaymentResponse
import com.bbmovie.payment.service.payment.dto.PaymentVerification
import com.bbmovie.payment.service.payment.dto.RefundResponse
import com.paypal.api.payments.*
import com.paypal.base.rest.APIContext
import com.paypal.base.rest.PayPalRESTException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("paypalProvider")
class PayPalAdapter() : PaymentProviderAdapter {

    @Value("\${payment.paypal.client-id}")
    private lateinit var clientId: String

    @Value("\${payment.paypal.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${payment.paypal.mode}")
    private lateinit var mode: String

    private val log = LoggerFactory.getLogger(PayPalAdapter::class.java)

    private fun getApiContext(): APIContext {
        return APIContext(clientId, clientSecret, mode)
    }

    override fun processPayment(request: PaymentRequest, httpServletRequest: HttpServletRequest): PaymentResponse {
        val payment = createPayment(request)

        return try {
            val createdPayment = payment.create(getApiContext())
            PaymentResponse(
                transactionId = createdPayment.id,
                status = if (createdPayment.state.equals(PaypalTransactionStatus.APPROVED.status, ignoreCase = true))
                    PaymentStatus.SUCCEEDED
                else
                    PaymentStatus.PENDING,
                providerReference = createdPayment.id
            )
        } catch (e: PayPalRESTException) {
            log.error("Unable to create PayPal payment", e)
            throw PayPalPaymentException("Unable to create PayPal payment")
        }
    }

    override fun verifyPayment(
        paymentData: MutableMap<String, String>,
        httpServletRequest: HttpServletRequest
    ): PaymentVerification {
        return try {
            val paymentId = paymentData["paymentId"]
                ?: throw PayPalPaymentException("Missing paymentId in verification data")
            val payment = Payment.get(getApiContext(), paymentId)
            PaymentVerification(
                success = payment.state.equals(PaypalTransactionStatus.APPROVED.status, ignoreCase = true),
                transactionId = payment.id
            )
        } catch (e: PayPalRESTException) {
            log.error("Unable to verify PayPal payment", e)
            throw PayPalPaymentException("Unable to verify PayPal payment")
        }
    }

    override fun refundPayment(
        paymentId: String,
        httpServletRequest: HttpServletRequest
    ): RefundResponse {
        return try {
            val sale = Sale.get(getApiContext(), paymentId)
            val refundRequest = RefundRequest()
            val refund = sale.refund(getApiContext(), refundRequest)
            RefundResponse(
                refundId = refund.id,
                status = if (refund.state == PaypalTransactionStatus.COMPLETED.status)
                    PaymentStatus.SUCCEEDED.status
                else
                    PaymentStatus.FAILED.status
            )
        } catch (e: PayPalRESTException) {
            log.error("Unable to refund PayPal payment", e)
            throw PayPalPaymentException("Unable to refund PayPal payment")
        }
    }

    override fun getPaymentProviderName(): PaymentProvider {
        return PaymentProvider.PAYPAL
    }

    private fun createPayment(request: PaymentRequest): Payment {
        val amount = Amount().apply {
            currency = request.currency
            total = request.amount?.toString()
        }

        val transaction = Transaction().apply {
            this.amount = amount
            description = "Order ${request.orderId}"
        }

        val payer = Payer().apply {
            paymentMethod = "paypal"
        }

        val redirectUrls = RedirectUrls().apply {
            returnUrl = "http://localhost:8080/api/payment/success"
            cancelUrl = "http://localhost:8080/api/payment/cancel"
        }

        return Payment().apply {
            intent = "sale"
            this.payer = payer
            transactions = listOf(transaction)
            this.redirectUrls = redirectUrls
        }
    }
}
