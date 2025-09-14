package com.bbmovie.payment.service.payment.provider.zalopay;

import com.bbmovie.payment.config.payment.ZaloPayProperties;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.entity.enums.SupportedCurrency;
import com.bbmovie.payment.exception.TransactionExpiredException;
import com.bbmovie.payment.exception.ZalopayException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.bbmovie.payment.service.SubscriptionPlanService;
import com.bbmovie.payment.service.payment.PricingService;
import com.bbmovie.payment.service.PaymentRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.stringToJsonNode;
import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.toJsonString;

@Log4j2
@Service("zalopay")
public class ZaloPayAdapter implements PaymentProviderAdapter {

    private String createOrderUrl;
    private String appIdParam;
    private String appUserParam;
    private String appTimeParam;
    private String amountParam;
    private String appTransIdParam;
    private String embedDataParam;
    private String itemParam;
    private String macParam;
    private String bankCodeParam;
    private String returnCodeParam;
    private String orderUrlParam;
    @SuppressWarnings("all")
    private String zpTransTokenParam;
    private String returnMessageParam;
    private String redirectUrlParam;
    private String callbackUrlParam;

    @PostConstruct
    public void init() {
        switch (properties.getVersion()) {
            case 1 -> {
                createOrderUrl = properties.isSandbox()
                        ? ZaloPayConstraint.CREATE_ORDER_URL_SANDBOX_V1
                        : ZaloPayConstraint.CREATE_ORDER_URL_PROD_V1;
                appIdParam = "appid";
                appUserParam = "appuser";
                appTimeParam = "apptime";
                amountParam = "amount";
                appTransIdParam = "apptransid";
                embedDataParam = "embeddata";
                itemParam = "item";
                macParam = "mac";
                bankCodeParam = null;
                returnCodeParam = "returncode";
                orderUrlParam = "orderurl";
                zpTransTokenParam = null;
                returnMessageParam = "returnmessage";
                callbackUrlParam = "callbackurl";
                redirectUrlParam = "redirecturl";
            }
            case 2 -> {
                createOrderUrl = properties.isSandbox()
                        ? ZaloPayConstraint.CREATE_ORDER_URL_SANDBOX_V2
                        : ZaloPayConstraint.CREATE_ORDER_URL_PROD_V2;
                appIdParam = "app_id";
                appUserParam = "app_user";
                appTimeParam = "app_time";
                amountParam = "amount";
                appTransIdParam = "app_trans_id";
                embedDataParam = "embed_data";
                itemParam = "item";
                macParam = "mac";
                bankCodeParam = "bank_code";
                returnCodeParam = "return_code";
                orderUrlParam = "order_url";
                zpTransTokenParam = "zp_trans_token";
                returnMessageParam = "return_message";
                callbackUrlParam = "callback_url";
                redirectUrlParam = "redirect_url";
            }
            default -> {
                log.fatal("Shut down the application: Unsupported ZaloPay version: {}", properties.getVersion());
                throw new IllegalArgumentException("Unsupported ZaloPay version: " + properties.getVersion());
            }
        }
    }

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PricingService pricingService;
    private final PaymentRecordService paymentRecordService;
    private final ZaloPayProperties properties;

    @Autowired
    public ZaloPayAdapter(
            PaymentTransactionRepository paymentTransactionRepository,
            SubscriptionPlanService subscriptionPlanService,
            PricingService pricingService,
            PaymentRecordService paymentRecordService,
            ZaloPayProperties properties
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.subscriptionPlanService = subscriptionPlanService;
        this.pricingService = pricingService;
        this.paymentRecordService = paymentRecordService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public PaymentCreationResponse createPaymentRequest(String userId, SubscriptionPaymentRequest request, HttpServletRequest hsr) {
        SubscriptionPlan plan = subscriptionPlanService.getById(UUID.fromString(request.subscriptionPlanId()));

        com.bbmovie.payment.dto.PricingBreakdown breakdown = pricingService.calculate(
                plan, request.billingCycle(), SupportedCurrency.VND.unit(), userId, null, request.voucherCode()
        );
        BigDecimal amount = breakdown.finalPrice();

        long appTime = System.currentTimeMillis();
        String appTransId = generateAppTransId();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String embedData = objectMapper.writeValueAsString(Map.of(
                    redirectUrlParam, properties.getRedirectUrl(),
                    "merchantinfo", request.subscriptionPlanId()
            ));

            String items = objectMapper.writeValueAsString(List.of(Map.of(
                    "itemid", request.subscriptionPlanId(),
                    "itemname", "Subscription",
                    "itemprice", amount.multiply(BigDecimal.ONE).longValue(),
                    "itemquantity", 1
            )));

            Map<String, String> params = new TreeMap<>();
            params.put(appIdParam, properties.getAppId());
            params.put(appUserParam, "user");
            params.put(appTimeParam, String.valueOf(appTime));
            params.put(amountParam, String.valueOf(amount.longValue()));
            params.put(appTransIdParam, appTransId);
            params.put(embedDataParam, embedData);
            params.put(itemParam, items);
            params.put(callbackUrlParam, properties.getCallbackUrl());

            if (properties.getVersion() == 2 && bankCodeParam != null) {
                params.put(bankCodeParam, "zalopayapp");
                params.put("description", "Subscription");
            }

            String rawData = String.join("|",
                    params.get(appIdParam),
                    params.get(appTransIdParam),
                    params.get(appUserParam),
                    params.get(amountParam),
                    params.get(appTimeParam),
                    params.get(embedDataParam),
                    params.get(itemParam)
            );

            String mac = ZaloHmacUtil.hmacHexStringEncode(ZaloHmacUtil.HMACSHA256, properties.getKey1(), rawData);
            params.put(macParam, mac);

            List<NameValuePair> form = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                form.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            HttpPost post = new HttpPost(createOrderUrl);
            post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

            String responseBody;
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post)) {
                responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }

            log.info("ZaloPay response: {}", responseBody);

            @SuppressWarnings("all")
            Map<String, Object> result = new ObjectMapper().readValue(responseBody, Map.class);

            String returnCode = result.get(returnCodeParam).toString();
            String returnMessage = result.get(returnMessageParam).toString();
            log.info("ZaloPay return code: {}, message: {}", returnCode, returnMessage);

            String orderUrl = result.get(orderUrlParam).toString();

            PaymentStatus status = ("1".equals(returnCode))
                    ? PaymentStatus.PENDING
                    : PaymentStatus.FAILED;

            if (returnCode.equals("2")) {
                log.info("Failed to create order, return code: {}", returnCode);
            }

            PaymentTransaction saved = paymentRecordService.createPendingTransaction(
                    userId,
                    plan,
                    amount,
                    SupportedCurrency.VND.unit(),
                    PaymentProvider.ZALOPAY,
                    appTransId,
                    "ZaloPay subscription"
            );

            return PaymentCreationResponse.builder()
                    .provider(PaymentProvider.ZALOPAY)
                    .serverTransactionId(String.valueOf(saved.getId()))
                    .providerTransactionId(appTransId)
                    .serverStatus(status)
                    .providerPaymentLink(orderUrl)
                    .build();
        } catch (IOException e) {
            log.error("Error processing ZaloPay payment", e);
            throw new ZalopayException("Error processing ZaloPay payment");
        }
    }

    @Override
    @Transactional
    public PaymentVerificationResponse handleCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String macFromRequest = paymentData.get("mac");
        String data = paymentData.get("data");

        if (macFromRequest == null) {
            macFromRequest = paymentData.get("checksum");
        }

        if (macFromRequest == null) {
            return PaymentVerificationResponse.builder()
                    .isValid(false)
                    .transactionId(null)
                    .code("INVALID")
                    .message("Missing data/mac")
                    .clientResponse(data)
                    .providerResponse(stringToJsonNode(toJsonString(Map.of(
                            returnCodeParam, -1,
                            returnMessageParam, "Missing data/mac"
                    ))))
                    .build();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = new ObjectMapper().readValue(data, Map.class);

            // Support both v1 and v2 field names
            String appTransId = asString(decoded.get(appTransIdParam), decoded.get("apptransid"), decoded.get("app_trans_id"));
            String returnCode = asString(decoded.get(returnCodeParam), decoded.get("returncode"), decoded.get(returnCodeParam));
            boolean success = "1".equals(returnCode) || "0".equals(returnCode);

            // Update DB transaction by appTransId only if still pending (prevent replay abuse)
            if (appTransId != null) {
                updateTransaction(appTransId, returnCode, success);
            }

            return PaymentVerificationResponse.builder()
                    .isValid(success)
                    .transactionId(appTransId)
                    .code(returnCode)
                    .message(success ? "SUCCESS" : "FAILED")
                    .clientResponse(data)
                    .providerResponse(stringToJsonNode(toJsonString(Map.of(
                            returnCodeParam, success ? 1 : -1,
                            returnMessageParam, success ? "OK" : "FAILED"
                    ))))
                    .build();
        } catch (Exception e) {
            log.error("Error verifying ZaloPay payment", e);
            return PaymentVerificationResponse.builder()
                    .isValid(false)
                    .transactionId(null)
                    .code("PARSE_ERROR")
                    .message("Invalid data payload")
                    .clientResponse(data)
                    .providerResponse(stringToJsonNode(toJsonString(Map.of(
                            returnCodeParam, -1,
                            returnMessageParam, "Invalid data payload"
                    ))))
                    .build();
        }
    }

    @Override
    public Object queryPayment(String userId, String paymentId) {
        throw new UnsupportedOperationException("Query payment is not supported by ZaloPay");
    }

    @Override
    public RefundResponse refundPayment(String userId, String paymentId, HttpServletRequest hsr) {
        throw new UnsupportedOperationException("Refund is not supported by ZaloPay");
    }

    private void updateTransaction(String appTransId, String returnCode, boolean success) {
        paymentTransactionRepository.findByProviderTransactionId(appTransId).ifPresent(tx -> {
            LocalDateTime now = LocalDateTime.now();
            if (tx.getExpiresAt() != null && now.isAfter(tx.getExpiresAt())) {
                throw new TransactionExpiredException("Payment expired");
            }

            if (tx.getStatus() != PaymentStatus.PENDING) {
                return;
            }
            tx.setResponseCode(returnCode);
            tx.setStatus(success ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
            paymentTransactionRepository.save(tx);
        });
    }

    private String asString(Object... candidates) {
        if (candidates == null) return null;
        for (Object c : candidates) {
            if (c != null) return String.valueOf(c);
        }
        return null;
    }

    private String generateAppTransId() {
        ZonedDateTime nowVN = ZonedDateTime.now(ZoneId.of(ZaloPayConstraint.VIETNAM_TZ));
        String yymmdd = nowVN.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return yymmdd + "_" + properties.getAppId() + "_" + suffix;
    }
}
