package com.bbmovie.payment.service.zalopay;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.bbmovie.payment.service.zalopay.ZaloPayQueryParams.*;

@Service("zalopayProvider")
public class ZaloPayAdapter implements PaymentProviderAdapter {

    @Value("${payment.zalopay.app-id}")
    private String appId;

    @Value("${payment.zalopay.key1}")
    private String key1;

    @Value("${payment.zalopay.key2}")
    private String key2;

    @Value("${payment.zalopay.sandbox:true}")
    private boolean sandbox = true;

    @Value("${payment.zalopay.callback-url}")
    private String callbackUrl;

    private static final Logger log = LoggerFactory.getLogger(ZaloPayAdapter.class);

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
            params.put(APP_ID, appId);
            params.put(APP_USER, request.getUserId() != null ? request.getUserId() : "user");
            params.put(APP_TIME, String.valueOf(appTime));
            params.put(AMOUNT, String.valueOf(amount.longValue()));
            params.put(APP_TRANS_ID, appTransId);
            params.put(EMBED_DATA, embedData);
            params.put(ITEM, items);

            // mac = HMAC-SHA256(appid|apptransid|appuser|amount|apptime|embeddata|item, key1)
            String rawData = String.join("|",
                    params.get(APP_ID),
                    params.get(APP_TRANS_ID),
                    params.get(APP_USER),
                    params.get(AMOUNT),
                    params.get(APP_TIME),
                    params.get(EMBED_DATA),
                    params.get(ITEM)
            );

            String mac = ZaloHmacUtil.hmacHexStringEncode(ZaloHmacUtil.HMACSHA256, key1, rawData);
            params.put(ZaloPayQueryParams.MAC, mac);

            List<NameValuePair> form = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                form.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            String url = sandbox ? ZaloPayConstraint.CREATE_ORDER_URL_SANDBOX : ZaloPayConstraint.CREATE_ORDER_URL_PROD;
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

            String responseBody;
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post)) {
                responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = new ObjectMapper().readValue(responseBody, Map.class);

            String returnCode = result.get("returncode") != null ? result.get("returncode").toString() : "-1";
            String orderUrl = result.get("orderurl") != null ? result.get("orderurl").toString() : null;

            PaymentStatus status = ("1".equals(returnCode) || "0".equals(returnCode)) ?
                    PaymentStatus.PENDING : PaymentStatus.FAILED;

            return new PaymentResponse(appTransId, status, orderUrl);

        } catch (IOException e) {
            throw new RuntimeException("Error processing ZaloPay payment", e);
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

            String appTransId = decoded.get("apptransid") != null ? decoded.get("apptransid").toString() : null;
            String returnCode = decoded.get("returncode") != null ? decoded.get("returncode").toString() : null;
            boolean success = "1".equals(returnCode) || "0".equals(returnCode);

            return new PaymentVerification(success, appTransId, null, null);
        } catch (IOException e) {
            return new PaymentVerification(false, null, null, null);
        }
    }

    @Override
    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
        return null;
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
