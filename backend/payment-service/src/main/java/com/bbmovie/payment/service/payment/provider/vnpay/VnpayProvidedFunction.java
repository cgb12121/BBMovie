package com.bbmovie.payment.service.payment.provider.vnpay;

import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.exception.VNPayException;
import com.bbmovie.payment.utils.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.bbmovie.payment.service.payment.provider.vnpay.VnpayQueryParams.*;

@SuppressWarnings("all")
@Log4j2
@Component
public class VnpayProvidedFunction {

    public String hashAllFields(Map fields, String hashSecret) {
        List fieldNames = new ArrayList(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        return hmacSHA512(hashSecret, sb.toString());
    }

    public String hmacSHA512(final String key, final String data) {
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
            throw new VNPayException("Unable to create payment.");
        }
    }

    public String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getLocalAddr();
            }
        } catch (Exception e) {
            ipAddress = "Invalid IP:" + e.getMessage();
        }
        return ipAddress;
    }

    public String createOrder(
            HttpServletRequest request, String amount, String vnp_TxnRef, String orderInfor,
            String tmpCode, String returnUrl, String hashSecret, String payUrl
    ) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_IpAddr = getIpAddress(request);
        String vnp_TmnCode = tmpCode;
        String orderType = "order-type";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put(VNPAY_VERSION_PARAM, vnp_Version);
        vnp_Params.put(VNPAY_COMMAND_PARAM, vnp_Command);
        vnp_Params.put(VNPAY_TMN_CODE_PARAM, vnp_TmnCode);
        vnp_Params.put(VNPAY_AMOUNT_PARAM, String.valueOf(amount));
        vnp_Params.put(VNPAY_CURRENCY_PARAM, "VND");

        vnp_Params.put(VNPAY_TXN_REF_PARAM, vnp_TxnRef);
        vnp_Params.put(VNPAY_ORDER_INFO_PARAM, orderInfor);
        vnp_Params.put(VNPAY_ORDER_TYPE_PARAM, orderType);

        vnp_Params.put(VNPAY_LOCALE_PARAM, "vn");

        vnp_Params.put(VNPAY_RETURN_URL_PARAM, returnUrl);
        vnp_Params.put(VNPAY_IP_ADDRESS_PARAM, vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put(VNPAY_CREATE_DATE_PARAM, vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put(VNPAY_EXPIRE_DATE_PARAM, vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
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
        String salt = hashSecret;
        String vnp_SecureHash = hmacSHA512(salt, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = payUrl + "?" + queryUrl;
        return paymentUrl;
    }

    public Map<String, String> createQueryOrder(PaymentTransaction txn, String tmpCode, String hashSecret) {
        String vnpTxnRef = RandomUtil.getRandomNumber(6);
        String transactionNo = txn.getProviderTransactionId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnpTransactionDate = txn.getTransactionDate().format(formatter);

        String vnpRequestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String vnpVersion = "2.1.0";
        String vnpCommand = "querydr";
        String vnpIpAddr = "127.0.0.1";
        String vnpCreateDate = formatter.format(LocalDateTime.now());
        String vnpOrderInfo = "Query transaction " + vnpTxnRef;

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", vnpRequestId);
        body.put("vnp_Version", vnpVersion);
        body.put("vnp_Command", vnpCommand);
        body.put("vnp_TmnCode", tmpCode);
        body.put("vnp_TxnRef", vnpTxnRef);
        body.put("vnp_OrderInfo", vnpOrderInfo);
        body.put("vnp_TransactionDate", vnpTransactionDate);
        body.put("vnp_CreateDate", vnpCreateDate);
        body.put("vnp_IpAddr", vnpIpAddr);

        String data = String.join("|",
                vnpRequestId, vnpVersion, vnpCommand, tmpCode,
                vnpTxnRef, vnpTransactionDate, vnpCreateDate, vnpIpAddr, vnpOrderInfo
        );

        String vnpSecureHash = hmacSHA512(hashSecret, data);
        body.put("vnp_SecureHash", vnpSecureHash);

        return body;
    }

    public Map<String, String> createRefundOrder(HttpServletRequest httpServletRequest, PaymentTransaction txn, String tmpCode, String hashSecret) {
        String requestId = RandomUtil.getRandomNumber(6);
        String vnpayVersion = "2.1.0";
        String vnpCommand = "querydr";
        String vnpIpAddr = "127.0.0.1";
        String vnpTxnRef = txn.getProviderTransactionId();
        String vnpTmnCode = tmpCode;
        String vnpOrderInfo = "Refund transaction: " + vnpTxnRef;
        BigDecimal amount = txn.getBaseAmount().multiply(BigDecimal.valueOf(100));
        
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());

        String transactionDate = formatter.format(txn.getTransactionDate());

        String data = String.join("|",
            requestId, vnpayVersion, vnpCommand, tmpCode,
             vnpTxnRef, transactionDate, vnp_CreateDate, vnpIpAddr, vnpOrderInfo
        );
        String vnpSecureHash = hmacSHA512(hashSecret, data);

        Map<String, String> body = new HashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", vnpayVersion);
        body.put("vnp_Command", vnpCommand);
        body.put("vnp_TmnCode", tmpCode);
        body.put("vnp_TxnRef", vnpTxnRef);
        body.put("vnp_Amount", String.valueOf(amount));
        body.put("vnp_OrderInfo", vnpOrderInfo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", vnp_CreateDate);
        body.put("vnp_IpAddr", vnpIpAddr);
        body.put("vnp_SecureHash", vnpSecureHash);

        return body;
    }

    public Map<String, String> executeRequest(Map<String, String> body, String apiUrl, String hashSecret) {
        Map<String, String> vpnQueryResult = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(body);

            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(apiUrl);
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpClient closeable = client; CloseableHttpResponse response = closeable.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                vpnQueryResult = mapper.readValue(responseBody, Map.class);
                String checksum = vpnQueryResult.get(VNPAY_SECURE_HASH);
                vpnQueryResult.remove(VNPAY_SECURE_HASH);

                String calculatedHash = hashAllFields(vpnQueryResult, hashSecret);
                if (!checksum.equals(calculatedHash) && checksum != null) {
                    log.error("Invalid hash when verify vnpay: {}", checksum);
                    throw new VNPayException("Unable to verify payment");
                }

                return vpnQueryResult;
            }
        } catch (Exception ex) {
            log.error("Failed to execute query request: {}", ex.getMessage());
            return vpnQueryResult;
        }
    }
}