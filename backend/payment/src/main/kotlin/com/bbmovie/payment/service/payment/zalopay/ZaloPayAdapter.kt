package com.bbmovie.payment.service.payment.zalopay

import com.bbmovie.payment.entity.PaymentProvider
import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.service.payment.PaymentProviderAdapter
import com.bbmovie.payment.service.payment.dto.PaymentRequest
import com.bbmovie.payment.service.payment.dto.PaymentResponse
import com.bbmovie.payment.service.payment.dto.PaymentVerification
import com.bbmovie.payment.service.payment.dto.RefundResponse
import com.bbmovie.payment.service.payment.zalopay.ZaloPayQueryParams.AMOUNT
import com.bbmovie.payment.service.payment.zalopay.ZaloPayQueryParams.APP_ID
import com.bbmovie.payment.service.payment.zalopay.ZaloPayQueryParams.APP_TIME
import com.bbmovie.payment.service.payment.zalopay.ZaloPayQueryParams.APP_TRANS_ID
import com.bbmovie.payment.service.payment.zalopay.ZaloPayQueryParams.APP_USER
import com.bbmovie.payment.service.payment.zalopay.ZaloPayQueryParams.EMBED_DATA
import com.bbmovie.payment.service.payment.zalopay.ZaloPayQueryParams.ITEM
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service("zalopayProvider")
class ZaloPayAdapter : PaymentProviderAdapter {

    @Value("\${payment.zalopay.app-id}")
    private lateinit var appId: String

    @Value("\${payment.zalopay.key1}")
    private lateinit var key1: String

    @Value("\${payment.zalopay.key2}")
    private lateinit var key2: String

    @Value("\${payment.zalopay.sandbox:true}")
    private var sandbox: Boolean = true

    @Value("\${payment.zalopay.callback-url}")
    private lateinit var callbackUrl: String

    private val log = LoggerFactory.getLogger(ZaloPayAdapter::class.java)

    override fun processPayment(request: PaymentRequest, httpServletRequest: HttpServletRequest): PaymentResponse {
        val amount = requireNotNull(request.amount) { "amount is required" }
        val currency = request.currency ?: ZaloPayConstraint.ONLY_SUPPORTED_CURRENCY
        require(currency == ZaloPayConstraint.ONLY_SUPPORTED_CURRENCY) { "ZaloPay supports only VND" }

        val appTime = System.currentTimeMillis()
        val appTransId = generateAppTransId()

        val embedData = ObjectMapper().writeValueAsString(mapOf(
            "redirecturl" to callbackUrl,
            "merchantinfo" to (request.orderId ?: "")
        ))

        val items = ObjectMapper().writeValueAsString(listOf(mapOf(
            "itemid" to (request.orderId ?: "order"),
            "itemname" to "Payment for order ${request.orderId}",
            "itemprice" to amount.multiply(BigDecimal(1)).toLong(),
            "itemquantity" to 1
        )))

        val params = sortedMapOf(
            APP_ID to appId,
            APP_USER to (request.userId ?: "user"),
            APP_TIME to appTime.toString(),
            AMOUNT to amount.toLong().toString(),
            APP_TRANS_ID to appTransId,
            EMBED_DATA to embedData,
            ITEM to items
        )

        // mac = HMAC-SHA256(appid|apptransid|appuser|amount|apptime|embeddata|item, key1)
        val rawData = listOf(
            params[APP_ID],
            params[APP_TRANS_ID],
            params[APP_USER],
            params[AMOUNT],
            params[APP_TIME],
            params[EMBED_DATA],
            params[ITEM]
        ).joinToString("|")

        val mac = ZaloHmacUtil.hmacHexStringEncode(ZaloHmacUtil.HMACSHA256, key1, rawData)
        params[ZaloPayQueryParams.MAC] = mac

        val form = params.entries.map { BasicNameValuePair(it.key, it.value) }
        val client = HttpClients.createDefault()
        val post = HttpPost(if (sandbox) ZaloPayConstraint.CREATE_ORDER_URL_SANDBOX else ZaloPayConstraint.CREATE_ORDER_URL_PROD)
        post.entity = UrlEncodedFormEntity(form, StandardCharsets.UTF_8)

        val responseBody = client.use {
            val response: CloseableHttpResponse = it.execute(post)
            EntityUtils.toString(response.entity, StandardCharsets.UTF_8)
        }

        // ZaloPay returns JSON: { orderurl, apptransid, returncode, ... }
        @Suppress("UNCHECKED_CAST")
        val result: Map<String, Any> = ObjectMapper().readValue(
            responseBody,
            Map::class.java
        ) as Map<String, Any>

        val returnCode = (result["returncode"]?.toString() ?: "-1")
        val orderUrl = result["orderurl"]?.toString()

        val status = if (returnCode == "1" || returnCode == "0") PaymentStatus.PENDING else PaymentStatus.FAILED
        return PaymentResponse(
            transactionId = appTransId,
            status = status,
            providerReference = orderUrl
        )
    }

    override fun verifyPayment(
        paymentData: MutableMap<String, String>,
        httpServletRequest: HttpServletRequest
    ): PaymentVerification {
        // ZaloPay callback sends JSON: { data: base64/json-string, mac: hmac }
        val mac = paymentData[ZaloPayConstraint.CALLBACK_MAC]
        val data = paymentData[ZaloPayConstraint.CALLBACK_DATA]
        if (mac.isNullOrBlank() || data.isNullOrBlank()) {
            return PaymentVerification(false, null)
        }

        val calculated = ZaloHmacUtil.hmacHexStringEncode(ZaloHmacUtil.HMACSHA256, key2, data)
        val isValid = calculated.equals(mac, ignoreCase = true)

        if (!isValid) return PaymentVerification(false, null)

        @Suppress("UNCHECKED_CAST")
        val decoded: Map<String, Any> = ObjectMapper().readValue(
            data,
            Map::class.java
        ) as Map<String, Any>

        val appTransId = decoded["apptransid"]?.toString()
        val returnCode = decoded["returncode"]?.toString()
        val success = returnCode == "1" || returnCode == "0"
        return PaymentVerification(success, appTransId)
    }

    override fun refundPayment(paymentId: String, httpServletRequest: HttpServletRequest): RefundResponse {
        throw UnsupportedOperationException("Refund is not supported by ZaloPay")
    }

    override fun getPaymentProviderName(): PaymentProvider = PaymentProvider.ZALO_PAY

    private fun generateAppTransId(): String {
        val nowVN: ZonedDateTime = ZonedDateTime.now(ZoneId.of(ZaloPayConstraint.VIETNAM_TZ))
        val yymmdd = nowVN.format(DateTimeFormatter.ofPattern("yyMMdd"))
        val suffix = UUID.randomUUID().toString().replace("-", "").take(10)
        return "${yymmdd}_${appId}_$suffix"
    }
}


