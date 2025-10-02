package com.bbmovie.payment.service.payment.provider.momo;

import com.bbmovie.payment.config.payment.MomoProperties;
import com.bbmovie.payment.dto.PaymentCreatedEvent;
import com.bbmovie.payment.dto.PricingBreakdown;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.entity.enums.SupportedCurrency;
import com.bbmovie.payment.exception.MomoException;
import com.bbmovie.payment.exception.PaymentCacheException;
import com.bbmovie.payment.exception.TransactionExpiredException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.bbmovie.payment.service.SubscriptionPlanService;
import com.bbmovie.payment.service.cache.RedisService;
import com.bbmovie.payment.service.nats.PaymentEventProducer;
import com.bbmovie.payment.service.payment.PricingService;
import com.bbmovie.payment.service.PaymentRecordService;
import com.bbmovie.payment.service.i18n.PaymentI18nService;
import com.example.common.utils.IpAddressUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

import static com.bbmovie.payment.service.payment.provider.momo.MomoParams.*;

@Slf4j
@Service("momo")
public class MomoAdapter implements PaymentProviderAdapter {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PricingService pricingService;
    private final PaymentRecordService paymentRecordService;
    private final PaymentI18nService paymentI18nService;
    private final ObjectMapper objectMapper;
    private final MomoProperties properties;
    private final RedisService redisService;
    private final PaymentEventProducer paymentEventProducer;

    @Autowired
    public MomoAdapter(
            PaymentTransactionRepository paymentTransactionRepository,
            SubscriptionPlanService subscriptionPlanService,
            @Qualifier("paymentObjectMapper") ObjectMapper objectMapper,
            MomoProperties properties,
            PricingService pricingService,
            PaymentRecordService paymentRecordService,
            PaymentI18nService paymentI18nService,
            RedisService redisService,
            PaymentEventProducer paymentEventProducer
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.subscriptionPlanService = subscriptionPlanService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.pricingService = pricingService;
        this.paymentRecordService = paymentRecordService;
        this.paymentI18nService = paymentI18nService;
        this.redisService = redisService;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Override
    @Transactional(noRollbackFor = PaymentCacheException.class)
    public PaymentCreationResponse createPaymentRequest(String userId, String userEmail, SubscriptionPaymentRequest request, HttpServletRequest hsr) {
        SubscriptionPlan plan = subscriptionPlanService.getById(UUID.fromString(request.subscriptionPlanId()));

        PricingBreakdown breakdown = pricingService.calculate(
                plan, request.billingCycle(), SupportedCurrency.VND.unit(),
                userId, IpAddressUtils.getClientIp(hsr), request.voucherCode()
        );
        BigDecimal amountInVnd = breakdown.finalPrice();

        String orderId = "Partner_Transaction_ID_" + System.currentTimeMillis();
        String requestId = "Request_ID_" + System.currentTimeMillis();
        String orderInfo = "Subscription";
        String requestType = MomoConstraint.REQUEST_TYPE_CAPTURE_WALLET;

        String extraDataJson;
        try {
            extraDataJson = objectMapper.writeValueAsString(Map.of("skus", ""));
        } catch (Exception e) {
            log.error("Failed to serialize extraData: {}", e.getMessage());
            throw new MomoException("Failed to serialize extraData");
        }
        String extraData = Base64.getEncoder().encodeToString(extraDataJson.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put(PARTNER_CODE, properties.getPartnerCode());
        body.put("storeName", properties.getStoreName());
        body.put("storeId", properties.getStoreId());
        body.put(REQUEST_ID, requestId);
        body.put(AMOUNT, amountInVnd.longValue());
        body.put(ORDER_ID, orderId);
        body.put(ORDER_INFO, orderInfo);
        body.put(REDIRECT_URL, properties.getRedirectUrl());
        body.put(IPN_URL, properties.getIpnUrl());
        body.put(REQUEST_TYPE, requestType);
        body.put(EXTRA_DATA, extraData);

        String rawSignature = buildSignatureString(Map.of(
                ACCESS_KEY, properties.getAccessKey(),
                AMOUNT, String.valueOf(amountInVnd.longValue()),
                EXTRA_DATA, extraData,
                IPN_URL, properties.getIpnUrl(),
                ORDER_ID, orderId,
                ORDER_INFO, orderInfo,
                PARTNER_CODE, properties.getPartnerCode(),
                REDIRECT_URL, properties.getRedirectUrl(),
                REQUEST_ID, requestId,
                REQUEST_TYPE, requestType
        ));

        String signature = hmacSha256Hex(properties.getSecretKey(), rawSignature);
        body.put(SIGNATURE, signature);

        String url = properties.isSandbox()
                ? MomoConstraint.CREATE_URL_TEST
                : MomoConstraint.CREATE_URL_PROD;
        String responseBody = sendJson(url, body);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

            int resultCode = Optional.ofNullable((Number) response.get(RESULT_CODE))
                    .map(Number::intValue)
                    .orElse(-1);
            String payUrl = Optional.ofNullable(response.get(PAY_URL))
                    .map(Object::toString)
                    .orElse(null);

            PaymentStatus status = (resultCode == 0)
                    ? PaymentStatus.PENDING
                    : PaymentStatus.FAILED;

            PaymentCreatedEvent paymentCreatedEvent = new PaymentCreatedEvent(
                    userId, userEmail, plan, breakdown.finalPrice(), SupportedCurrency.VND.unit(),
                    PaymentProvider.MOMO, orderId, orderInfo
            );

            PaymentTransaction transaction = paymentRecordService.createPendingTransaction(paymentCreatedEvent);
            redisService.cache(paymentCreatedEvent);

            return PaymentCreationResponse.builder()
                    .provider(PaymentProvider.MOMO)
                    .serverTransactionId(String.valueOf(transaction.getId()))
                    .providerTransactionId(orderId)
                    .serverStatus(status)
                    .providerPaymentLink(payUrl)
                    .build();
        } catch (Exception e) {
            log.error("Failed to process payment: {}", e.getMessage());
            throw new MomoException("Failed to process payment");
        }
    }

    @Override
    public PaymentVerificationResponse handleCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String signature = paymentData.get("signature");
        Integer resultCode = Optional.ofNullable(paymentData.get(RESULT_CODE))
                .map(Integer::parseInt)
                .orElse(null);
        if (signature == null || signature.isBlank() || resultCode == null) {
            throw new MomoException("Missing signature or result code");
        }

        boolean isIpn = paymentData.containsKey(TRANS_ID);
        String rawSignature;
        if (isIpn) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put(ACCESS_KEY, properties.getAccessKey());
            params.put(AMOUNT, paymentData.getOrDefault(AMOUNT, ""));
            params.put(EXTRA_DATA, paymentData.getOrDefault(EXTRA_DATA, ""));
            params.put(MESSAGE, paymentData.getOrDefault(MESSAGE, ""));
            params.put(ORDER_ID, paymentData.getOrDefault(ORDER_ID, ""));
            params.put(ORDER_INFO, paymentData.getOrDefault(ORDER_INFO, ""));
            params.put(ORDER_TYPE, paymentData.getOrDefault(ORDER_TYPE, ""));
            params.put(PARTNER_CODE, properties.getPartnerCode());
            params.put(PAY_TYPE, paymentData.getOrDefault(PAY_TYPE, ""));
            params.put(REQUEST_ID, paymentData.getOrDefault(REQUEST_ID, ""));
            params.put(RESPONSE_TIME, paymentData.getOrDefault(RESPONSE_TIME, ""));
            params.put(RESULT_CODE, paymentData.getOrDefault(RESULT_CODE, ""));
            params.put(TRANS_ID, paymentData.getOrDefault(TRANS_ID, ""));

            rawSignature = buildSignatureString(params);
        } else {
            rawSignature = buildSignatureString(Map.of(
                    ACCESS_KEY, properties.getAccessKey(),
                    AMOUNT, paymentData.getOrDefault(AMOUNT, ""),
                    MESSAGE, paymentData.getOrDefault(MESSAGE, ""),
                    ORDER_ID, paymentData.getOrDefault(ORDER_ID, ""),
                    PARTNER_CODE, properties.getPartnerCode(),
                    PAY_URL, paymentData.getOrDefault(PAY_URL, ""),
                    REQUEST_ID, paymentData.getOrDefault(REQUEST_ID, ""),
                    RESPONSE_TIME, paymentData.getOrDefault(RESPONSE_TIME, ""),
                    RESULT_CODE, paymentData.getOrDefault(RESULT_CODE, "")
            ));
        }

        String calculatedSignature = hmacSha256Hex(properties.getSecretKey(), rawSignature);
        boolean match = calculatedSignature.equalsIgnoreCase(signature);
        boolean success = match && resultCode == 0;

        String orderId = paymentData.get(ORDER_ID);
        PaymentTransaction transaction = paymentTransactionRepository.findByProviderTransactionId(orderId)
                .orElseThrow(() -> new MomoException("Transaction not found: " + orderId));

        LocalDateTime now = LocalDateTime.now();
        if (transaction.getExpiresAt() != null && now.isAfter(transaction.getExpiresAt())) {
            throw new TransactionExpiredException("Payment expired");
        }

        String message = paymentI18nService.messageFor(PaymentProvider.MOMO,
                success ? "SUCCESS" : String.valueOf(resultCode));

        //TODO: finish
        paymentEventProducer.publishSubscriptionSuccessEvent(null);

        return new PaymentVerificationResponse(
                success,
                paymentData.get(ORDER_ID),
                String.valueOf(resultCode),
                message,
                null,
                null
        );
    }

    @Override
    public Object queryPayment(String userId, String paymentId) {
        throw new UnsupportedOperationException("This operation is not implemented for Momo");
    }

    @Override
    public RefundResponse refundPayment(String userId, String userEmail, String paymentId, HttpServletRequest hsr) {
        paymentEventProducer.publishSubscriptionCancelEvent(null);
        throw new UnsupportedOperationException("Refund is not supported by Momo");
    }

    private String buildSignatureString(Map<String, String> fields) {
        return fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElseThrow(() -> new MomoException("Failed to build signature string"));
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
            throw new MomoException("Failed to HMAC");
        }
    }

    private String sendJson(String url, Map<String, Object> body) {
        try (var client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Failed to send request to MoMo: {}", e.getMessage());
            throw new MomoException("Failed to send request to MoMo");
        }
    }
}
