package com.bbmovie.payment.service.momo;

import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

/**
 * MOMO banned the developer account
 */
@Slf4j
@Service("momo")
public class MomoAdapter implements PaymentProviderAdapter {

    @Value("${payment.momo.partner-code}")
    private String partnerCode;

    @Value("${payment.momo.access-key}")
    private String accessKey;

    @Value("${payment.momo.secret-key}")
    private String secretKey;

    @Value("${payment.momo.sandbox:true}")
    private boolean sandbox;

    @Value("${payment.momo.redirect-url}")
    private String redirectUrl;

    @Value("${payment.momo.ipn-url}")
    private String ipnUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    private final boolean supported = false;

    @Override
    public PaymentCreationResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        if (!supported) {
            throw new UnsupportedOperationException("Momo is not supported yet");
        }

        long amount = Optional.ofNullable(request.getAmount())
                .map(BigDecimal::longValue)
                .orElseThrow(() -> new IllegalArgumentException("amount is required"));
        String orderId = Optional.ofNullable(request.getOrderId())
                .orElse("Partner_Transaction_ID_" + System.currentTimeMillis());
        String requestId = "Request_ID_" + System.currentTimeMillis();
        String orderInfo = "Payment for order " + orderId;
        String requestType = MomoConstraint.REQUEST_TYPE_CAPTURE_WALLET;

        String extraDataJson;
        try {
            extraDataJson = mapper.writeValueAsString(Map.of("skus", ""));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize extraData", e);
        }
        String extraData = Base64.getEncoder().encodeToString(extraDataJson.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("requestType", requestType);
        body.put("extraData", extraData);

        String rawSignature = buildSignatureString(Map.of(
                "accessKey", accessKey,
                "amount", String.valueOf(amount),
                "extraData", extraData,
                "ipnUrl", ipnUrl,
                "orderId", orderId,
                "orderInfo", orderInfo,
                "partnerCode", partnerCode,
                "redirectUrl", redirectUrl,
                "requestId", requestId,
                "requestType", requestType
        ));

        String signature = hmacSha256Hex(secretKey, rawSignature);
        body.put("signature", signature);

        String url = sandbox ? MomoConstraint.CREATE_URL_TEST : MomoConstraint.CREATE_URL_PROD;
        String responseBody = sendJson(url, body);


        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = mapper.readValue(responseBody, Map.class);

            int resultCode = Optional.ofNullable((Number) response.get("resultCode")).map(Number::intValue).orElse(-1);
            String payUrl = Optional.ofNullable(response.get("payUrl")).map(Object::toString).orElse(null);

            PaymentStatus status = (resultCode == 0) ? PaymentStatus.PENDING : PaymentStatus.FAILED;

            return PaymentCreationResponse.builder()
                    .transactionId(orderId)
                    .status(status)
                    .providerReference(payUrl)
                    .build();
        } catch (Exception e) {
            log.error("Failed to process payment: {}", e.getMessage());
            return PaymentCreationResponse.builder()
                    .transactionId(null)
                    .status(null)
                    .providerReference(null)
                    .build();
        }
    }

    @Override
    public PaymentVerificationResponse verifyPaymentCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        if (!supported) {
            throw new UnsupportedOperationException("Momo is not supported yet");
        }

        String signature = paymentData.get("signature");
        Integer resultCode = Optional.ofNullable(paymentData.get("resultCode")).map(Integer::parseInt).orElse(null);
        if (signature == null || signature.isBlank() || resultCode == null) {
            return new PaymentVerificationResponse(false, null, null, null, null, null);
        }

        boolean isIpn = paymentData.containsKey("transId");
        String rawSignature;
        if (isIpn) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("accessKey", accessKey);
            params.put("amount", paymentData.getOrDefault("amount", ""));
            params.put("extraData", paymentData.getOrDefault("extraData", ""));
            params.put("message", paymentData.getOrDefault("message", ""));
            params.put("orderId", paymentData.getOrDefault("orderId", ""));
            params.put("orderInfo", paymentData.getOrDefault("orderInfo", ""));
            params.put("orderType", paymentData.getOrDefault("orderType", ""));
            params.put("partnerCode", partnerCode);
            params.put("payType", paymentData.getOrDefault("payType", ""));
            params.put("requestId", paymentData.getOrDefault("requestId", ""));
            params.put("responseTime", paymentData.getOrDefault("responseTime", ""));
            params.put("resultCode", paymentData.getOrDefault("resultCode", ""));
            params.put("transId", paymentData.getOrDefault("transId", ""));

            rawSignature = buildSignatureString(params);
        } else {
            rawSignature = buildSignatureString(Map.of(
                    "accessKey", accessKey,
                    "amount", paymentData.getOrDefault("amount", ""),
                    "message", paymentData.getOrDefault("message", ""),
                    "orderId", paymentData.getOrDefault("orderId", ""),
                    "partnerCode", partnerCode,
                    "payUrl", paymentData.getOrDefault("payUrl", ""),
                    "requestId", paymentData.getOrDefault("requestId", ""),
                    "responseTime", paymentData.getOrDefault("responseTime", ""),
                    "resultCode", paymentData.getOrDefault("resultCode", "")
            ));
        }

        String calculated = hmacSha256Hex(secretKey, rawSignature);
        boolean match = calculated.equalsIgnoreCase(signature);
        boolean success = match && resultCode == 0;

        return new PaymentVerificationResponse(success, paymentData.get("orderId"), null, null, null, null);
    }

    @Override
    public Object queryPaymentFromProvider(String paymentId, HttpServletRequest httpServletRequest) {
        if (!supported) {
            throw new UnsupportedOperationException("Momo is not supported yet");
        }
        return null;
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        if (!supported) {
            throw new UnsupportedOperationException("Momo is not supported yet");
        }
        throw new UnsupportedOperationException("Refund is not supported by Momo");
    }

    @Override
    public PaymentProvider getPaymentProviderName() {
        return PaymentProvider.MOMO;
    }

    private String buildSignatureString(Map<String, String> fields) {
        return fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
    }

    private String hmacSha256Hex(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Failed to HMAC: {}", ex.getMessage());
            return "";
        }
    }

    private String sendJson(String url, Map<String, Object> body) {
        try (var client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(mapper.writeValueAsString(body), StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send request to MoMo", e);
        }
    }
}
