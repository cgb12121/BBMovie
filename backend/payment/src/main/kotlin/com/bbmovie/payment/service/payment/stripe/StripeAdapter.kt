package com.bbmovie.payment.service.payment.stripe

import com.bbmovie.payment.entity.PaymentProvider
import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.entity.PaymentTransaction
import com.bbmovie.payment.exception.StripePaymentException
import com.bbmovie.payment.repository.PaymentTransactionRepository
import com.bbmovie.payment.service.payment.PaymentProviderAdapter
import com.bbmovie.payment.service.payment.dto.PaymentRequest
import com.bbmovie.payment.service.payment.dto.PaymentResponse
import com.bbmovie.payment.service.payment.dto.PaymentVerification
import com.bbmovie.payment.service.payment.dto.RefundResponse
import com.bbmovie.payment.service.payment.paypal.PayPalAdapter
import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.PaymentIntent
import com.stripe.model.Refund
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

@Service("stripeProvider")
class StripeAdapter(
    private val paymentTransactionRepository: PaymentTransactionRepository
) : PaymentProviderAdapter {

    @Value("\${payment.stripe.secret-key}")
    private lateinit var secretKey: String

    @Value("\${payment.stripe.publishable-key}")
    private lateinit var publishableKey: String

    private val log = LoggerFactory.getLogger(PayPalAdapter::class.java)

    init {
        Stripe.apiKey = secretKey
    }

    override fun processPayment(request: PaymentRequest, httpServletRequest: HttpServletRequest): PaymentResponse {
        log.info("Processing Stripe payment for order: ${request.orderId}")

        val transaction = PaymentTransaction(
            user = request.userId,
            amount = request.amount,
            currency = request.currency,
            paymentProvider = PaymentProvider.STRIPE,
            paymentMethod = request.paymentMethodId,
            transactionDate = LocalDateTime.now(),
            description = "Order ${request.orderId}"
        )

        return try {
            val params = mapOf(
                "amount" to request.amount?.multiply(BigDecimal(100))?.longValueExact(),
                "currency" to request.currency,
                "description" to "Order ${request.orderId}",
                "payment_method" to request.paymentMethodId,
                "confirmation_method" to "manual",
                "confirm" to true
            )

            val paymentIntent = PaymentIntent.create(params)
            val stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.status)

            transaction.paymentGatewayId = paymentIntent.id
            transaction.providerStatus = stripeStatus.status
            transaction.status = stripeStatus.paymentStatus

            paymentTransactionRepository.save(transaction)

            PaymentResponse(
                transactionId = paymentIntent.id,
                status = stripeStatus.paymentStatus,
                providerReference = paymentIntent.clientSecret
            )
        } catch (ex: StripeException) {
            log.error("Failed to process Stripe payment: ${ex.message}")
            transaction.status = PaymentStatus.FAILED
            transaction.errorCode = ex.code
            transaction.errorMessage = ex.message
            paymentTransactionRepository.save(transaction)
            throw StripePaymentException("Payment processing failed: ${ex.message}")
        }
    }

    override fun verifyPayment(
        paymentData: MutableMap<String, String>,
        httpServletRequest: HttpServletRequest
    ): PaymentVerification {
        val paymentId = paymentData["id"] ?: throw StripePaymentException("Missing payment ID")
        log.info("Verifying Stripe payment: $paymentId")

        return try {
            val paymentIntent = PaymentIntent.retrieve(paymentId)
            val stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.status)

            val transaction: PaymentTransaction? = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
                ?.orElseThrow { StripePaymentException("Transaction not found: $paymentId") }

            transaction?.providerStatus = stripeStatus.status
            transaction?.status = stripeStatus.paymentStatus
            paymentTransactionRepository.save(transaction)

            PaymentVerification(
                success = stripeStatus == StripeTransactionStatus.SUCCEEDED,
                transactionId = paymentId
            )
        } catch (ex: StripeException) {
            log.error("Failed to verify Stripe payment: ${ex.message}")
            throw StripePaymentException("Payment verification failed: ${ex.message}")
        }
    }

    override fun refundPayment(
        paymentId: String,
        httpServletRequest: HttpServletRequest
    ): RefundResponse {
        log.info("Processing Stripe refund for paymentId: $paymentId")

        val transaction = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
            ?.orElseThrow { RuntimeException("Transaction not found: $paymentId") }

        return try {
            val params = mapOf("payment_intent" to paymentId)
            val refund = Refund.create(params)

            val refundStatus = if (refund.status.equals("succeeded", ignoreCase = true)) {
                StripeTransactionStatus.SUCCEEDED
            } else {
                StripeTransactionStatus.fromStatus(refund.status)
            }

            transaction?.status = PaymentStatus.REFUNDED
            transaction?.providerStatus = refund.status
            paymentTransactionRepository.save(transaction)

            RefundResponse(
                refundId = refund.id,
                status = refundStatus.paymentStatus.status
            )
        } catch (ex: StripeException) {
            log.error("Failed to process Stripe refund: ${ex.message}")
            transaction?.errorCode = ex.code
            transaction?.errorMessage = ex.message
            paymentTransactionRepository.save(transaction)
            throw StripePaymentException("Refund processing failed: ${ex.message}")
        }
    }

    override fun getPaymentProviderName(): PaymentProvider {
        return PaymentProvider.STRIPE
    }
}
