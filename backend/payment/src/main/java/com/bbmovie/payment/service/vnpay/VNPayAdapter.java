package com.bbmovie.payment.service.vnpay;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.exception.VNPayException;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.bbmovie.payment.utils.IpAddressUtils;
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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.bbmovie.payment.service.vnpay.VNPayConfig.hashAllFields;
import static com.bbmovie.payment.service.vnpay.VNPayConfig.hmacSHA512;
import static com.bbmovie.payment.service.vnpay.VnPayQueryParams.*;

@Log4j2
@Service("vnpayProvider")
public class VNPayAdapter implements PaymentProviderAdapter {

    @Value("${payment.vnpay.TmnCode}")
    private String tmnCode;

    @Value("${payment.vnpay.HashSecret}")
    private String hashSecret;

    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        BigDecimal amountInVnd = request.getAmount();
        String amountStr = amountInVnd.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();
        String vnpTxnRef = VNPayConfig.getRandomNumber(8);
        String vnpIpAddr = VNPayConfig.getIpAddress(httpServletRequest);

        Map<String, String> vnpParams = new LinkedHashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Amount", amountStr);
        vnpParams.put("vnp_CurrCode", "VND");

        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "billpayment");
        vnpParams.put("vnp_OrderType", "order-type");

        vnpParams.put("vnp_Locale", "vn");

        vnpParams.put("vnp_ReturnUrl", "https://abc123.ngrok.io/api/payment/vnpay/return");
        vnpParams.put("vnp_IpAddr", vnpIpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnpParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

        return new PaymentResponse(vnpTxnRef, PaymentStatus.PENDING, paymentUrl);
    }



    @Override
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String vnpSecureHash = paymentData.get(VNPAY_SECURE_HASH);
        String vnpTxnRef = paymentData.get(VNPAY_TXN_REF_PARAM);
        paymentData.remove(VNPAY_SECURE_HASH);

        String calculatedHash = hashAllFields(paymentData);
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
        String vnpIpAddr = IpAddressUtils.getPublicServerIp();
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

        String secureHash = hashAllFields(vnpParams);
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

    @Override
    public PaymentProvider getPaymentProviderName() {
        return PaymentProvider.VNPAY;
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
        params.put("vnp_Command", VnPayCommand.QUERY_DR.getCommand());
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
                VnPayCommand.QUERY_DR.getCommand(),
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