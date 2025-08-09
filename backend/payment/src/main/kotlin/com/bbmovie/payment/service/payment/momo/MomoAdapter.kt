package com.bbmovie.payment.service.payment.momo

import com.bbmovie.payment.entity.PaymentProvider
import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.service.payment.PaymentProviderAdapter
import com.bbmovie.payment.service.payment.dto.PaymentRequest
import com.bbmovie.payment.service.payment.dto.PaymentResponse
import com.bbmovie.payment.service.payment.dto.PaymentVerification
import com.bbmovie.payment.service.payment.dto.RefundResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.http.HttpServletRequest
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service("momoProvider")
class MomoAdapter : PaymentProviderAdapter {

    @Value("\${payment.momo.partner-code}")
    private lateinit var partnerCode: String

    @Value("\${payment.momo.access-key}")
    private lateinit var accessKey: String

    @Value("\${payment.momo.secret-key}")
    private lateinit var secretKey: String

    @Value("\${payment.momo.sandbox:true}")
    private var sandbox: Boolean = true

    @Value("\${payment.momo.redirect-url}")
    private lateinit var redirectUrl: String

    @Value("\${payment.momo.ipn-url}")
    private lateinit var ipnUrl: String

    private val log = LoggerFactory.getLogger(MomoAdapter::class.java)
    private val mapper: ObjectMapper = jacksonObjectMapper()

    override fun processPayment(request: PaymentRequest, httpServletRequest: HttpServletRequest): PaymentResponse {
        val amount = requireNotNull(request.amount) { "amount is required" }.toLong()
        val orderId = request.orderId ?: ("Partner_Transaction_ID_" + System.currentTimeMillis())
        val requestId = "Request_ID_" + System.currentTimeMillis()
        val orderInfo = "Payment for order ${request.orderId ?: orderId}"
        val requestType = MomoConstraint.REQUEST_TYPE_CAPTURE_WALLET

        val extraDataJson = mapper.writeValueAsString(mapOf("skus" to ""))
        val extraData = Base64.getEncoder().encodeToString(extraDataJson.toByteArray(StandardCharsets.UTF_8))

        val body = mutableMapOf(
            "partnerCode" to partnerCode,
            "requestId" to requestId,
            "amount" to amount,
            "orderId" to orderId,
            "orderInfo" to orderInfo,
            "redirectUrl" to redirectUrl,
            "ipnUrl" to ipnUrl,
            "requestType" to requestType,
            "extraData" to extraData
        )

        val rawSignature = buildSignatureString(
            mapOf(
                "accessKey" to accessKey,
                "amount" to amount.toString(),
                "extraData" to extraData,
                "ipnUrl" to ipnUrl,
                "orderId" to orderId,
                "orderInfo" to orderInfo,
                "partnerCode" to partnerCode,
                "redirectUrl" to redirectUrl,
                "requestId" to requestId,
                "requestType" to requestType
            )
        )

        val signature = hmacSha256Hex(secretKey, rawSignature)
        body["signature"] = signature

        val responseBody = sendJson(
            MomoConstraint.CREATE_URL_TEST.takeIf { sandbox } ?: MomoConstraint.CREATE_URL_PROD, body
        )
        val response: Map<String, Any?> = mapper.readValue(responseBody)
        val resultCode = (response["resultCode"] as? Number)?.toInt() ?: -1
        val payUrl = response["payUrl"]?.toString()

        val status = if (resultCode == 0) PaymentStatus.PENDING else PaymentStatus.FAILED
        return PaymentResponse(
            transactionId = orderId,
            status = status,
            providerReference = payUrl
        )
    }

    override fun verifyPayment(
        paymentData: MutableMap<String, String>,
        httpServletRequest: HttpServletRequest
    ): PaymentVerification {
        val signature = paymentData["signature"]
        val resultCode = paymentData["resultCode"]?.toIntOrNull()
        if (signature.isNullOrBlank() || resultCode == null) {
            return PaymentVerification(false, null)
        }

        val isIpn = paymentData.containsKey("transId")

        val rawSignature = if (isIpn) {
            // IPN signature per docs
            buildSignatureString(
                mapOf(
                    "accessKey" to accessKey,
                    "amount" to (paymentData["amount"] ?: ""),
                    "extraData" to (paymentData["extraData"] ?: ""),
                    "message" to (paymentData["message"] ?: ""),
                    "orderId" to (paymentData["orderId"] ?: ""),
                    "orderInfo" to (paymentData["orderInfo"] ?: ""),
                    "orderType" to (paymentData["orderType"] ?: ""),
                    "partnerCode" to partnerCode,
                    "payType" to (paymentData["payType"] ?: ""),
                    "requestId" to (paymentData["requestId"] ?: ""),
                    "responseTime" to (paymentData["responseTime"] ?: ""),
                    "resultCode" to (paymentData["resultCode"] ?: ""),
                    "transId" to (paymentData["transId"] ?: "")
                )
            )
        } else {
            // Redirect/response signature per docs
            buildSignatureString(
                mapOf(
                    "accessKey" to accessKey,
                    "amount" to (paymentData["amount"] ?: ""),
                    "message" to (paymentData["message"] ?: ""),
                    "orderId" to (paymentData["orderId"] ?: ""),
                    "partnerCode" to partnerCode,
                    "payUrl" to (paymentData["payUrl"] ?: ""),
                    "requestId" to (paymentData["requestId"] ?: ""),
                    "responseTime" to (paymentData["responseTime"] ?: ""),
                    "resultCode" to (paymentData["resultCode"] ?: "")
                )
            )
        }

        val calculated = hmacSha256Hex(secretKey, rawSignature)
        val match = calculated.equals(signature, ignoreCase = true)
        val success = match && resultCode == 0
        val orderId = paymentData["orderId"]
        return PaymentVerification(success, orderId)
    }

    override fun refundPayment(paymentId: String, httpServletRequest: HttpServletRequest): RefundResponse {
        throw UnsupportedOperationException("Refund is not supported by Momo")
    }

    override fun getPaymentProviderName(): PaymentProvider = PaymentProvider.MOMO

    private fun buildSignatureString(fields: Map<String, String>): String {
        // MoMo requires keys sorted from a-z and joined with &
        return fields.toSortedMap().entries.joinToString("&") { (k, v) -> "$k=$v" }
    }

    private fun hmacSha256Hex(key: String, data: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            val bytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (ex: Exception) {
            log.error("Failed to HMAC: ${ex.message}")
            ""
        }
    }

    private fun sendJson(url: String, body: Map<String, Any?>): String {
        val client = HttpClients.createDefault()
        val post = HttpPost(url)
        post.setHeader("Content-Type", "application/json")
        post.entity = StringEntity(mapper.writeValueAsString(body), StandardCharsets.UTF_8)
        return client.use {
            val response: CloseableHttpResponse = it.execute(post)
            EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
        }
    }
}


