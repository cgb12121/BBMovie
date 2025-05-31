package com.example.bbmovie.service.payment.vnpay;

import com.example.bbmovie.exception.VNPayException;
import com.example.bbmovie.service.payment.PaymentProviderAdapter;
import com.example.bbmovie.service.payment.PaymentStatus;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.dto.PaymentVerification;
import com.example.bbmovie.service.payment.dto.RefundResponse;
import com.example.bbmovie.utils.IpAddressUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.bbmovie.service.payment.vnpay.VnPayQueryParams.*;

@Log4j2
@Service("vnpayProvider")
public class VNPayAdapter implements PaymentProviderAdapter {

    @Value("${payment.vnpay.TmnCode}")
    private String tmnCode;

    @Value("${payment.vnpay.HashSecret}")
    private String hashSecret;

    @Override
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        String orderId = request.getOrderId();
        String amount = request.getAmount().multiply(new BigDecimal(100)).toString();
        String vnpTxnRef = System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String vnpIpAddr = IpAddressUtils.getClientIp(httpServletRequest);
        String vnpCreateDate = new SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(new Date());

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put(VNPAY_VERSION_PARAM, VnPayConstraint.VERSION);
        vnpParams.put(VNPAY_COMMAND_PARAM, VnPayCommand.PAY.getCommand());
        vnpParams.put(VNPAY_TMN_CODE_PARAM, tmnCode);
        vnpParams.put(VNPAY_AMOUNT_PARAM, amount);
        vnpParams.put(VNPAY_CURRENCY_PARAM, VnPayConstraint.ONLY_SUPPORTED_CURRENCY);
        vnpParams.put(VNPAY_TXN_REF_PARAM, vnpTxnRef);
        vnpParams.put(VNPAY_ORDER_INFO_PARAM, "Order " + orderId);
        vnpParams.put(VNPAY_ORDER_TYPE_PARAM, VnPayOrderType.BILL_PAYMENT.getType());
        vnpParams.put(VNPAY_LOCALE_PARAM, VnPayConstraint.ONLY_SUPPORTED_CURRENCY);
        vnpParams.put(VNPAY_RETURN_URL_PARAM, VnPayConstraint.RETURN_URL);
        vnpParams.put(VNPAY_IP_ADDRESS_PARAM, vnpIpAddr);
        vnpParams.put(VNPAY_CREATE_DATE_PARAM, vnpCreateDate);

        String secureHash = generateSecureHash(vnpParams);
        vnpParams.put(VNPAY_SECURE_HASH, secureHash);

        String redirectUrl = VnPayConstraint.PAYMENT_URL + "?" + toQueryString(vnpParams);
        return new PaymentResponse(vnpTxnRef, PaymentStatus.PENDING, redirectUrl);
    }

    @Override
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String vnpSecureHash = paymentData.get(VNPAY_SECURE_HASH);
        String vnpTxnRef = paymentData.get(VNPAY_TXN_REF_PARAM);
        paymentData.remove(VNPAY_SECURE_HASH);

        String calculatedHash = generateSecureHash(paymentData);
        boolean isValid = vnpSecureHash.equals(calculatedHash)
                && VnPayTransactionStatus.SUCCESS.getCode().equals(paymentData.get(VNPAY_RESPONSE_CODE));
        return new PaymentVerification(isValid, vnpTxnRef);
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        // Retrieve original transaction details from the database or VNPay API
        // For simplicity, assume transaction details are stored or provided
        // Original transaction ID
        String vnpTransactionNo = getTransactionNo(paymentId); // Fetch from stored data or API
        String vnpTransactionDate = getTransactionDate(paymentId); // Fetch from stored data or API
        String vnpCreateDate = new SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(new Date());
        String vnpIpAddr = IpAddressUtils.getServerIp();
        String vnpAmount = "10000"; // Example: refund 100 VND (replace with actual amount * 100)

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put(VNPAY_VERSION_PARAM, VnPayConstraint.VERSION);
        vnpParams.put(VNPAY_COMMAND_PARAM, VnPayCommand.REFUND.getCommand());
        vnpParams.put(VNPAY_TRANSACTION_TYPE, "02"); // Full refund
        vnpParams.put(VNPAY_TMN_CODE_PARAM, tmnCode);
        vnpParams.put(VNPAY_TXN_REF_PARAM, paymentId);
        vnpParams.put(VNPAY_AMOUNT_PARAM, vnpAmount);
        vnpParams.put(VNPAY_TRANSACTION_NO, vnpTransactionNo);
        vnpParams.put(VNPAY_TRANSACTION_DATE, vnpTransactionDate);
        vnpParams.put(VNPAY_CREATE_DATE_PARAM, vnpCreateDate);
        vnpParams.put(VNPAY_IP_ADDRESS_PARAM, vnpIpAddr);
        vnpParams.put(VNPAY_ORDER_INFO_PARAM, "Refund for transaction " + paymentId);

        String secureHash = generateSecureHash(vnpParams);
        vnpParams.put(VNPAY_SECURE_HASH, secureHash);

        String response = sendRefundRequest(vnpParams);
        assert response != null;
        Map<String, String> responseParams = parseResponse(response);

        String responseCode = responseParams.get(VNPAY_RESPONSE_CODE);
        String refundId = responseParams.get(VNPAY_TRANSACTION_NO);
        return new RefundResponse(
                refundId,
                VnPayTransactionStatus.SUCCESS.getCode().equals(responseCode)
                    ? PaymentStatus.SUCCEEDED.getStatus()
                    : PaymentStatus.FAILED.getStatus()
        );
    }

    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
        String vnpRequestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String vnpCreateDate = new SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(new Date());
        String vnpTransactionDate = getTransactionDate(paymentId); // Fetch this from your DB
        String vnpIpAddr = IpAddressUtils.getClientIp(httpServletRequest);
        String vnpCreateBy = "system"; // Or authenticated admin username

        Map<String, String> params = new HashMap<>();
        params.put("vnp_RequestId", vnpRequestId);
        params.put("vnp_Version", VnPayConstraint.VERSION);
        params.put("vnp_Command", "querydr");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_TxnRef", paymentId);
        params.put("vnp_OrderInfo", "Query for transaction " + paymentId);
        params.put("vnp_TransactionDate", vnpTransactionDate);
        params.put("vnp_CreateBy", vnpCreateBy);
        params.put("vnp_CreateDate", vnpCreateDate);
        params.put("vnp_IpAddr", vnpIpAddr);

        String rawData = String.join("|",
                vnpRequestId,
                VnPayConstraint.VERSION,
                "querydr",
                tmnCode,
                paymentId,
                vnpTransactionDate,
                vnpCreateBy,
                vnpCreateDate,
                vnpIpAddr,
                "Query for transaction " + paymentId
        );

        String secureHash = hmacSHA512(hashSecret, rawData);
        params.put("vnp_SecureHash", secureHash);

        return sendPostRequest(params);
    }


    private String generateSecureHash(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        for (String fieldName : fieldNames) {
            sb.append(fieldName).append("=").append(params.get(fieldName)).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return hmacSHA512(hashSecret, sb.toString());
    }

    private String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }

    private String toQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry
                        .getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
                )
                .collect(Collectors.joining("&"));
    }

    private String sendRefundRequest(Map<String, String> params) throws VNPayException {
        String refundUrl = VnPayConstraint.TRANSACTION_URL;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(refundUrl);
            List<NameValuePair> nvps = new ArrayList<>();
            params.forEach((key, value) -> nvps.add(new BasicNameValuePair(key, value)));
            post.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            log.error("Failed to send refund request to VNPay: {}", ex.getMessage(), ex);
            throw new VNPayException("Refund request failed: " + ex.getMessage());
        }
    }

    private String sendPostRequest(Map<String, String> jsonParams) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(VnPayConstraint.TRANSACTION_URL);
            httpPost.setHeader("Content-Type", "application/json");

            String json = new ObjectMapper().writeValueAsString(jsonParams);
            httpPost.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            log.error("Failed to send post request to VNPay: {}", ex.getMessage(), ex);
            throw new VNPayException("Refund request failed: " + ex.getMessage());
        }
    }


    private Map<String, String> parseResponse(String response) {
        Map<String, String> result = new HashMap<>();
        for (String param : response.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                result.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    // Placeholder: Fetch vnp_TransactionNo from stored data or VNPay API
    private String getTransactionNo(String txnRef) {
        // In practice, query your database or VNPay's transaction query API
        return "VNP" + txnRef; // Example
    }

    // Placeholder: Fetch vnp_TransactionDate from stored data or VNPay API
    private String getTransactionDate(String txnRef) {
        log.info("getTransactionDat {}", txnRef);
        return new SimpleDateFormat(VnPayConstraint.DATE_FORMAT).format(new Date()); // Example
    }
}