package com.bbmovie.payment.service.payment.vnpay

import com.bbmovie.payment.entity.PaymentProvider
import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.exception.VNPayException
import com.bbmovie.payment.service.payment.PaymentProviderAdapter
import com.bbmovie.payment.service.payment.dto.PaymentRequest
import com.bbmovie.payment.service.payment.dto.PaymentResponse
import com.bbmovie.payment.service.payment.dto.PaymentVerification
import com.bbmovie.payment.service.payment.dto.RefundResponse
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_AMOUNT_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_COMMAND_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_CREATE_DATE_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_CURRENCY_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_IP_ADDRESS_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_LOCALE_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_ORDER_INFO_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_ORDER_TYPE_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_RESPONSE_CODE
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_RETURN_URL_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_SECURE_HASH
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_TMN_CODE_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_TRANSACTION_DATE
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_TRANSACTION_NO
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_TRANSACTION_TYPE
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_TXN_REF_PARAM
import com.bbmovie.payment.service.payment.vnpay.VnPayQueryParams.VNPAY_VERSION_PARAM
import com.bbmovie.payment.utils.IpAddressUtils
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service("vnpayProvider")
class VNPayAdapter() : PaymentProviderAdapter {

    @Value("\${payment.vnpay.TmnCode}") private lateinit var tmnCode: String
    @Value("\${payment.vnpay.HashSecret}") private lateinit var hashSecret: String

    private val log = LoggerFactory.getLogger(VNPayAdapter::class.java)

    override fun processPayment(
        request: PaymentRequest,
        httpServletRequest: HttpServletRequest
    ): PaymentResponse {
        val orderId = request.orderId
        val amount = request.amount?.multiply(BigDecimal(100)).toString()
        val vnpTxnRef = System.currentTimeMillis().toString() + UUID.randomUUID().toString().replace("-", "").take(32)
        val vnpIpAddr = IpAddressUtils.getClientIp(httpServletRequest)
        val vnpCreateDate = SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(Date())

        val vnpParams = sortedMapOf(
            VNPAY_VERSION_PARAM to VnPayConstraint.VERSION,
            VNPAY_COMMAND_PARAM to VnPayCommand.PAY.command,
            VNPAY_TMN_CODE_PARAM to tmnCode,
            VNPAY_AMOUNT_PARAM to amount,
            VNPAY_CURRENCY_PARAM to VnPayConstraint.ONLY_SUPPORTED_CURRENCY,
            VNPAY_TXN_REF_PARAM to vnpTxnRef,
            VNPAY_ORDER_INFO_PARAM to "Order $orderId",
            VNPAY_ORDER_TYPE_PARAM to VnPayOrderType.BILL_PAYMENT.type,
            VNPAY_LOCALE_PARAM to VnPayConstraint.ONLY_SUPPORTED_CURRENCY,
            VNPAY_RETURN_URL_PARAM to VnPayConstraint.RETURN_URL,
            VNPAY_IP_ADDRESS_PARAM to vnpIpAddr,
            VNPAY_CREATE_DATE_PARAM to vnpCreateDate
        )

        vnpParams[VNPAY_SECURE_HASH] = generateSecureHash(vnpParams)

        val redirectUrl = VnPayConstraint.PAYMENT_URL + "?" + toQueryString(vnpParams)
        return PaymentResponse(vnpTxnRef, PaymentStatus.PENDING, redirectUrl)
    }

    override fun verifyPayment(
        paymentData: MutableMap<String, String>,
        httpServletRequest: HttpServletRequest
    ): PaymentVerification {
        val data = paymentData.toMutableMap()
        val vnpSecureHash = data.remove(VNPAY_SECURE_HASH)
        val vnpTxnRef = data[VNPAY_TXN_REF_PARAM]

        val calculatedHash = generateSecureHash(data)
        val isValid = vnpSecureHash == calculatedHash &&
                VnPayTransactionStatus.SUCCESS.code == data[VNPAY_RESPONSE_CODE]
        return PaymentVerification(isValid, vnpTxnRef)
    }

    override fun refundPayment(
        paymentId: String,
        httpServletRequest: HttpServletRequest
    ): RefundResponse {
        val vnpTransactionNo = getTransactionNo(paymentId)
        val vnpTransactionDate = getTransactionDate(paymentId)
        val vnpCreateDate = SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(Date())
        val vnpIpAddr = IpAddressUtils.getPublicServerIp()
        val vnpAmount = "10000" // Example

        val vnpParams = sortedMapOf(
            VNPAY_VERSION_PARAM to VnPayConstraint.VERSION,
            VNPAY_COMMAND_PARAM to VnPayCommand.REFUND.command,
            VNPAY_TRANSACTION_TYPE to "02",
            VNPAY_TMN_CODE_PARAM to tmnCode,
            VNPAY_TXN_REF_PARAM to paymentId,
            VNPAY_AMOUNT_PARAM to vnpAmount,
            VNPAY_TRANSACTION_NO to vnpTransactionNo,
            VNPAY_TRANSACTION_DATE to vnpTransactionDate,
            VNPAY_CREATE_DATE_PARAM to vnpCreateDate,
            VNPAY_IP_ADDRESS_PARAM to vnpIpAddr,
            VNPAY_ORDER_INFO_PARAM to "Refund for transaction $paymentId"
        )

        vnpParams[VNPAY_SECURE_HASH] = generateSecureHash(vnpParams)

        val response = sendRefundRequest(vnpParams) ?: throw VNPayException("Refund request failed")
        val responseParams = parseResponse(response)

        val responseCode = responseParams[VNPAY_RESPONSE_CODE]
        val refundId = responseParams[VNPAY_TRANSACTION_NO]

        val status = if (VnPayTransactionStatus.SUCCESS.code == responseCode)
            PaymentStatus.SUCCEEDED.status
        else PaymentStatus.FAILED.status

        return RefundResponse(refundId ?: "", status)
    }

    override fun getPaymentProviderName(): PaymentProvider {
        return PaymentProvider.VNPAY
    }

    fun queryPayment(paymentId: String, request: HttpServletRequest): Any {
        val vnpRequestId = UUID.randomUUID().toString().replace("-", "").take(32)
        val vnpCreateDate = SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(Date())
        val vnpTransactionDate = getTransactionDate(paymentId)
        val vnpIpAddr = IpAddressUtils.getClientIp(request)
        val vnpCreateBy = "system"

        val params = mutableMapOf(
            "vnp_RequestId" to vnpRequestId,
            "vnp_Version" to VnPayConstraint.VERSION,
            "vnp_Command" to VnPayCommand.QUERY_DR.command,
            "vnp_TmnCode" to tmnCode,
            "vnp_TxnRef" to paymentId,
            "vnp_OrderInfo" to "Query for transaction $paymentId",
            "vnp_TransactionDate" to vnpTransactionDate,
            "vnp_CreateBy" to vnpCreateBy,
            "vnp_CreateDate" to vnpCreateDate,
            "vnp_IpAddr" to vnpIpAddr
        )

        val rawData = listOf(
            vnpRequestId,
            VnPayConstraint.VERSION,
            VnPayCommand.QUERY_DR.command,
            tmnCode,
            paymentId,
            vnpTransactionDate,
            vnpCreateBy,
            vnpCreateDate,
            vnpIpAddr,
            "Query for transaction $paymentId"
        ).joinToString("|")

        params["vnp_SecureHash"] = hmacSHA512(hashSecret, rawData)

        return sendPostRequest(params) as Any
    }

    private fun generateSecureHash(params: Map<String, String>): String {
        val sortedKeys = params.keys.sorted()
        val sb = StringBuilder()
        for (key in sortedKeys) {
            sb.append("$key=${params[key]}&")
        }
        sb.setLength(sb.length - 1)
        return hmacSHA512(hashSecret, sb.toString())
    }

    private fun hmacSHA512(key: String?, data: String?): String {
        return try {
            requireNotNull(key)
            requireNotNull(data)
            val hmac = Mac.getInstance("HmacSHA512")
            hmac.init(SecretKeySpec(key.toByteArray(), "HmacSHA512"))
            val bytes = hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (ex: Exception) {
            log.error("Failed to generate secure hash: ${ex.message}")
            return ""
        }
    }

    private fun toQueryString(params: Map<String, String>): String {
        return params.entries.joinToString("&") {
            "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}"
        }
    }

    private fun sendRefundRequest(params: Map<String, String>): String? {
        val client = HttpClients.createDefault()
        val post = HttpPost(VnPayConstraint.TRANSACTION_URL)
        val nvps = params.map { BasicNameValuePair(it.key, it.value) }
        post.entity = UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8)
        return client.use {
            val response: CloseableHttpResponse = it.execute(post)
            EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
        }
    }

    private fun sendPostRequest(jsonParams: Map<String, String>): String? {
        val client = HttpClients.createDefault()
        val post = HttpPost(VnPayConstraint.TRANSACTION_URL)
        post.setHeader("Content-Type", "application/json")
        post.entity = StringEntity(ObjectMapper().writeValueAsString(jsonParams), StandardCharsets.UTF_8)
        return client.use {
            val response = it.execute(post)
            EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
        }
    }

    private fun parseResponse(response: String): Map<String, String> {
        return response.split("&").mapNotNull {
            val (key, value) = it.split("=").let { parts -> parts.getOrNull(0) to parts.getOrNull(1) }
            if (key != null && value != null) key to URLDecoder.decode(value, StandardCharsets.UTF_8) else null
        }.toMap()
    }

    private fun getTransactionNo(txnRef: String): String = "VNP$txnRef"

    private fun getTransactionDate(txnRef: String): String {
        log.info("getTransactionDate {}", txnRef)
        return SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(Date())
    }
}
