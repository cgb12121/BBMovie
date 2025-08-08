package com.bbmovie.payment.service.payment

import com.bbmovie.payment.entity.PaymentProvider
import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.entity.PaymentTransaction
import com.bbmovie.payment.repository.PaymentTransactionRepository
import com.bbmovie.payment.service.payment.dto.PaymentRequest
import com.bbmovie.payment.service.payment.dto.PaymentResponse
import com.bbmovie.payment.service.payment.dto.PaymentVerification
import com.bbmovie.payment.service.payment.dto.RefundResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import java.util.function.Consumer

@Service
class PaymentService @Autowired constructor(
    private val providers: MutableMap<String?, PaymentProviderAdapter>,
    private val paymentTransactionRepository: PaymentTransactionRepository
) {

    fun getProvider(name: String?): PaymentProviderAdapter? {
        return providers[name]
    }

    fun processPayment(
        providerName: String,
        request: PaymentRequest,
        httpServletRequest: HttpServletRequest
    ): PaymentResponse? {
        val provider: PaymentProviderAdapter? = providers[providerName]

        val response: PaymentResponse? = provider?.processPayment(request, httpServletRequest)
        val entity = PaymentTransaction(
            user = null,
            amount = null,
            currency = null,
            paymentProvider = provider?.getPaymentProviderName() as PaymentProvider,
            paymentMethod = null,
            transactionDate = LocalDateTime.now(),
            description = ""
        )

        paymentTransactionRepository.save(entity)
        return response
    }

    fun verifyPayment(
        providerName: String,
        paymentData: MutableMap<String, String>,
        httpServletRequest: HttpServletRequest
    ): PaymentVerification {
        val provider: PaymentProviderAdapter? = providers[providerName]
        requireNotNull(provider) { "Provider $providerName not supported" }

        val verification: PaymentVerification = provider.verifyPayment(paymentData, httpServletRequest)
        if (verification.success) {
            paymentTransactionRepository.findById(UUID.fromString(verification.transactionId))
                .ifPresent(Consumer { entity: PaymentTransaction? ->
                    entity?.status = PaymentStatus.SUCCEEDED
                    paymentTransactionRepository.save(entity as PaymentTransaction)
                })
        }
        return verification
    }

    fun refundPayment(
        providerName: String,
        paymentId: String,
        request: HttpServletRequest
    ): RefundResponse? {
        val provider: PaymentProviderAdapter? = providers[providerName]
        requireNotNull(provider) { "Provider $providerName not supported" }

        val response: RefundResponse = provider.refundPayment(paymentId, request)
        if ("SUCCESS" == response.status) {
            paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .ifPresent(Consumer { entity: PaymentTransaction? ->
                    entity?.status = PaymentStatus.REFUNDED
                    paymentTransactionRepository.save(entity as PaymentTransaction)
                })
        }
        return response
    }
}