package com.bbmovie.payment.service.zalopay;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private String returnMessageParam;

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
                returnMessageParam = "returnmessage";
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
                returnMessageParam = "return_message";
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
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        BigDecimal amount = Objects.requireNonNull(request.getAmount(), "amount is required");
        String currency = request.getCurrency() != null
                ? request.getCurrency()
                : ZaloPayConstraint.ONLY_SUPPORTED_CURRENCY;

        if (!currency.equals(ZaloPayConstraint.ONLY_SUPPORTED_CURRENCY)) {
            throw new ZalopayException("ZaloPay supports only VND");
        }

        long appTime = System.currentTimeMillis();
        String appTransId = generateAppTransId();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String embedData = objectMapper.writeValueAsString(Map.of(
                    "redirecturl", callbackUrl,
                    "merchantinfo", request.getOrderId()
            ));

            String items = objectMapper.writeValueAsString(List.of(Map.of(
                    "itemid", request.getOrderId(),
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
                params.put("description", "Payment for order " + (request.getOrderId()));
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

            @SuppressWarnings("all")
            /*
              Tham số api trả về:

                return_code	                Int	                1: Thành công
                                                                2: Thất bại
                return_message	            String	            Mô tả mã trạng thái
                sub_return_code	            Int	                Mã trạng thái chi tiết
                sub_return_message	        String	            Mô tả chi tiết mã trạng thái
                order_url	                String	            Dùng để tạo QR code hoặc gọi chuyển tiếp sang trang cổng ZaloPay
                zp_trans_token	            String	            Thông tin token đơn hàng
                order_token	                String	            Thông tin token đơn hàng
                qr_code                 	String	            Dùng để tạo NAPAS VietQR trên hệ thống Merchant. NAPAS VietQR là một trong những giải pháp thanh toán hoàn toàn mới,
                                                                chấp nhận thanh toán được thực hiện bởi cả ZaloPay & +40 ngân hàng thuộc hệ thống NAPAS. Người dùng có thể sử dụng
                                                                ứng dụng ngân hàng quét NAPAS VietQR để thanh toán

                Ví dụ:

                {
                  "return_code": 1,
                  "return_message": "Giao dịch thành công",
                  "sub_return_code": 1,
                  "sub_return_message": "Giao dịch thành công",
                  "zp_trans_token": "ACSMARlbXkIzcSCNHDWC_5jA",
                  "order_url": "https://qcgateway.zalopay.vn/openinapp?order=eyJ6cHRyYW5zdG9rZW4iOiJBQ1NNQVJsYlhrSXpjU0NOSERXQ181akEiLCJhcHBpZCI6MTI0NzA1fQ==",
                  "order_token": "ACSMARlbXkIzcSCNHDWC_5jA",
                  "qr_code": "00020101021226520010vn.zalopay0203001010627000503173307089089161731338580010A000000727012800069704540114998002401295460208QRIBFTTA5204739953037045405690005802VN62210817330708908916173136304409F"
                }

            */
            Map<String, Object> result = new ObjectMapper().readValue(responseBody, Map.class);

            String returnCode = result.get(returnCodeParam).toString();
            String returnMessage = result.get(returnMessageParam) != null ? result.get(returnMessageParam).toString() : null;
            log.info("ZaloPay return code: {}, message: {}", returnCode, returnMessage);

            String paramKey = version == 1 ? orderUrlParam : zpTransTokenParam;
            String orderUrl = result.get(paramKey) != null ? result.get(paramKey).toString() : null;

            PaymentStatus status = ("1".equals(returnCode))
                    ? PaymentStatus.PENDING
                    : PaymentStatus.FAILED;

            if (returnCode.equals("2")) {
                log.info("Failed to create order, return code: {}", returnCode);
            }

            PaymentTransaction transaction = createZalopayTransaction(request, appTransId, orderUrl, status, returnCode);
            paymentTransactionRepository.save(transaction);

            return new PaymentResponse(appTransId, status, orderUrl);
        } catch (IOException e) {
            log.error("Error processing ZaloPay payment", e);
            throw new ZalopayException("Error processing ZaloPay payment");
        }
    }

    @Override
    @Transactional
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        @SuppressWarnings("all")
        /*
            Dữ liệu nhận được từ callback:
            data	        Json String	        Dữ liệu giao dịch ZaloPay gọi về cho ứng dụng
            mac	            String	            Thông tin chứng thực của đơn hàng, dùng Callback Key (Key2) được cung cấp để chứng thực đơn hàng
            type	        Int	                Loại callback               1: Order
                                                                            2: Agreement
         */
        // ZaloPay callback (v1/v2): form-urlencoded with keys data, mac


        String macFromRequest = paymentData.get("mac");
        String data = paymentData.get("data");
        if (macFromRequest == null || macFromRequest.isBlank() || data == null || data.isBlank()) {
            return new PaymentVerification(false, null, "INVALID", "Missing data/mac");
        }

        // Verify with key2
        String calculated = ZaloHmacUtil.hmacHexStringEncode(ZaloHmacUtil.HMACSHA256, key2, data);
        if (calculated == null || !calculated.equalsIgnoreCase(macFromRequest)) {
            return new PaymentVerification(false, null, "SIGNATURE_MISMATCH", "MAC not match");
        }


        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> decoded = new ObjectMapper().readValue(data, Map.class);

            // Support both v1 and v2 field names
            String appTransId = asString(decoded.get(appTransIdParam), decoded.get("apptransid"), decoded.get("app_trans_id"));
            String returnCode = asString(decoded.get(returnCodeParam), decoded.get("returncode"), decoded.get(returnCodeParam));
            boolean success = "1".equals(returnCode) || "0".equals(returnCode);

            // Update DB transaction by appTransId
            if (appTransId != null) {
                paymentTransactionRepository.findByPaymentGatewayId(appTransId).ifPresent(tx -> {
                    tx.setProviderStatus(returnCode);
                    tx.setStatus(success ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
                    paymentTransactionRepository.save(tx);
                });
            }

            @SuppressWarnings("all")
            /*
                Thông tin AppServer trả về cho ZaloPayServer khi nhận callback#
                Content-Type: application/json
                Tham số	Kiểu dữ liệu	Ý nghĩa
                return_code	                Int                 1: thành công
                                                                2: trùng mã giao dịch ZaloPay zptransid hoặc app_trans_id
                                                                ( đã cung cấp dịch vụ cho user trước đó)
                                                                <>: thất bại (không callback lại)
                return_message	            String	            Mô tả chi tiết mã trạng thái


                Ví dụ:

                {
                  "return_code": "[return_code]",
                  "return_message": "[return_message]"
                }
             */
            String s = "this is just a placeholder to suppress comment warnings. need to perform post request after call back";

            return new PaymentVerification(success, appTransId, returnCode, success ? "SUCCESS" : "FAILED");
        } catch (Exception e) {
            log.error("Error verifying ZaloPay payment", e);
            return new PaymentVerification(false, null, "PARSE_ERROR", "Invalid data payload");
        }
    }

    private String asString(Object... candidates) {
        if (candidates == null) return null;
        for (Object c : candidates) {
            if (c != null) return String.valueOf(c);
        }
        return null;
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

    private PaymentTransaction createZalopayTransaction(
            PaymentRequest request, String appTransId, String orderUrl,
            PaymentStatus status, String returnCode
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(request.getUserId());
        transaction.setSubscription(null);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPaymentProvider(PaymentProvider.ZALO_PAY);
        transaction.setPaymentMethod(request.getPaymentMethodId());
        transaction.setPaymentGatewayId(appTransId);
        transaction.setPaymentGatewayOrderId(orderUrl); // v1: orderurl, v2: zp_trans_token
        transaction.setProviderStatus(status == PaymentStatus.PENDING ? "PENDING" : returnCode);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(status);
        transaction.setDescription("ZaloPay payment for order: " + request.getOrderId());
        transaction.setIpnUrl(callbackUrl);
        transaction.setReturnUrl(orderUrl);
        return transaction;
    }
}
