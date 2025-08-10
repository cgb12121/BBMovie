package com.bbmovie.payment.service.zalopay;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.ZalopayException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Log4j2
@Service("zalopay")
public class ZaloPayAdapter implements PaymentProviderAdapter {

    @Value("${payment.zalopay.app-id}")
    private String appId;

    @Value("${payment.zalopay.key1}")
    private String key1;

    @Value("${payment.zalopay.key2}")
    private String key2;

    @Value("${payment.zalopay.version}")
    private int version;

    @Value("${payment.zalopay.sandbox:true}")
    private boolean sandbox;

    @Value("${payment.zalopay.callback-url}")
    private String callbackUrl;

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
    private String zpTransTokenParam;
    private String returnCodeKeyInCallback;

    @PostConstruct
    public void init() {
        switch (version) {
            case 1 -> {
                createOrderUrl = sandbox
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
                returnCodeKeyInCallback = "returncode";
            }
            case 2 -> {
                createOrderUrl = sandbox
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
                returnCodeKeyInCallback = "return_code";
            }
            default -> {
                log.fatal("Shut down the application: Unsupported ZaloPay version: {}", version);
                throw new IllegalArgumentException("Unsupported ZaloPay version: " + version);
            }
        }
    }

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public ZaloPayAdapter(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        BigDecimal amount = Objects.requireNonNull(request.getAmount(), "amount is required");
        String currency = request.getCurrency() != null ? request.getCurrency() : ZaloPayConstraint.ONLY_SUPPORTED_CURRENCY;
        if (!currency.equals(ZaloPayConstraint.ONLY_SUPPORTED_CURRENCY)) {
            throw new IllegalArgumentException("ZaloPay supports only VND");
        }

        long appTime = System.currentTimeMillis();
        String appTransId = generateAppTransId();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String embedData = objectMapper.writeValueAsString(Map.of(
                    "redirecturl", callbackUrl,
                    "merchantinfo", request.getOrderId() != null ? request.getOrderId() : ""
            ));

            String items = objectMapper.writeValueAsString(List.of(Map.of(
                    "itemid", request.getOrderId() != null ? request.getOrderId() : "order",
                    "itemname", "Payment for order " + request.getOrderId(),
                    "itemprice", amount.multiply(BigDecimal.ONE).longValue(),
                    "itemquantity", 1
            )));

            Map<String, String> params = new TreeMap<>();
            params.put(appIdParam, appId);
            params.put(appUserParam, request.getUserId() != null ? request.getUserId() : "user");
            params.put(appTimeParam, String.valueOf(appTime));
            params.put(amountParam, String.valueOf(amount.longValue()));
            params.put(appTransIdParam, appTransId);
            params.put(embedDataParam, embedData);
            params.put(itemParam, items);

            if (version == 2 && bankCodeParam != null) {
                params.put(bankCodeParam, "zalopayapp");
                params.put("description", "Payment for order " + (request.getOrderId() != null ? request.getOrderId() : ""));
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

            String mac = ZaloHmacUtil.hmacHexStringEncode(ZaloHmacUtil.HMACSHA256, key1, rawData);
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

            @SuppressWarnings("unchecked")
            Map<String, Object> result = new ObjectMapper().readValue(responseBody, Map.class);

            String returnCode = result.get(returnCodeParam) != null ? result.get(returnCodeParam).toString() : "-1";

            String paramKey = version == 1 ? orderUrlParam : zpTransTokenParam;
            String orderUrl = result.get(paramKey) != null ? result.get(paramKey).toString() : null;

            PaymentStatus status = ("1".equals(returnCode) || "0".equals(returnCode)) ?
                    PaymentStatus.PENDING : PaymentStatus.FAILED;

            return new PaymentResponse(appTransId, status, orderUrl);

        } catch (IOException e) {
            log.error("Error processing ZaloPay payment", e);
            throw new ZalopayException("Error processing ZaloPay payment");
        }
    }

    @Override
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String mac = paymentData.get(ZaloPayConstraint.CALLBACK_MAC);
        String data = paymentData.get(ZaloPayConstraint.CALLBACK_DATA);
        if (mac == null || mac.isBlank() || data == null || data.isBlank()) {
            return new PaymentVerification(false, null, null, null);
        }

        String calculated = ZaloHmacUtil.hmacHexStringEncode(ZaloHmacUtil.HMACSHA256, key2, data);
        boolean isValid = calculated != null && calculated.equalsIgnoreCase(mac);

        if (!isValid) {
            return new PaymentVerification(false, null, null, null);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = new ObjectMapper().readValue(data, Map.class);

            String appTransId = decoded.get(appTransIdParam) != null ? decoded.get(appTransIdParam).toString() : null;
            String returnCode = decoded.get(returnCodeKeyInCallback) != null ? decoded.get(returnCodeKeyInCallback).toString() : null;
            boolean success = "1".equals(returnCode) || "0".equals(returnCode);

            return new PaymentVerification(success, appTransId, null, null);
        } catch (IOException e) {
            return new PaymentVerification(false, null, null, null);
        }
    }

    @Override
    public Object queryPaymentFromProvider(String paymentId, HttpServletRequest httpServletRequest) {
        throw new UnsupportedOperationException("Query payment is not supported by ZaloPay");
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        throw new UnsupportedOperationException("Refund is not supported by ZaloPay");
    }

    @Override
    public PaymentProvider getPaymentProviderName() {
        return PaymentProvider.ZALO_PAY;
    }

    private String generateAppTransId() {
        ZonedDateTime nowVN = ZonedDateTime.now(ZoneId.of(ZaloPayConstraint.VIETNAM_TZ));
        String yymmdd = nowVN.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return yymmdd + "_" + appId + "_" + suffix;
    }
}
