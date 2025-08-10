package com.bbmovie.payment.service.vnpay;

import com.bbmovie.payment.entity.PaymentTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.bbmovie.payment.service.vnpay.VnPayQueryParams.*;

@SuppressWarnings("all")
@Log4j2
public class VNPayUtils {

    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_Returnurl = "http:localhost:8088/api/payment/vnpay/callback";
    public static String vnp_TmnCode = "8WXB2M3M"; // Got on random github
    public static String vnp_HashSecret = "L9DBOO6OZ0TN8J8BVZJ5BXT0X6UY83RC";// Got on random github
    public static String vnp_apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    public static String hashAllFields(Map fields) {
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
        return hmacSHA512(vnp_HashSecret, sb.toString());
    }

    public static String hmacSHA512(final String key, final String data) {
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

    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getLocalAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String createOrder(HttpServletRequest request, String amount, String vnp_TxnRef, String orderInfor) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_IpAddr = VNPayUtils.getIpAddress(request);
        String vnp_TmnCode = VNPayUtils.vnp_TmnCode;
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

        vnp_Params.put(VNPAY_RETURN_URL_PARAM, vnp_Returnurl);
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
        String salt = VNPayUtils.vnp_HashSecret;
        String vnp_SecureHash = VNPayUtils.hmacSHA512(salt, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayUtils.vnp_PayUrl + "?" + queryUrl;
        return paymentUrl;
    }

    public static Map<String, String> createQueryOrder(
            HttpServletRequest httpServletRequest, PaymentTransaction txn
    ) {
        String vnpTxnRef = txn.getPaymentGatewayId();
        String transactionNo = txn.getPaymentGatewayOrderId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnpTransactionDate = txn.getTransactionDate().format(formatter);

        String vnpRequestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String vnpVersion = "2.1.0";
        String vnpCommand = "querydr";
        String vnpIpAddr = VNPayUtils.getIpAddress(httpServletRequest);
        String vnpCreateDate = formatter.format(LocalDateTime.now());
        String vnpOrderInfo = "Query transaction " + vnpTxnRef;

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", vnpRequestId);
        body.put("vnp_Version", vnpVersion);
        body.put("vnp_Command", vnpCommand);
        body.put("vnp_TmnCode", VNPayUtils.vnp_TmnCode);
        body.put("vnp_TxnRef", vnpTxnRef);
        body.put("vnp_OrderInfo", vnpOrderInfo);
        body.put("vnp_TransactionDate", vnpTransactionDate);
        body.put("vnp_CreateDate", vnpCreateDate);
        body.put("vnp_IpAddr", vnpIpAddr);

        List fieldNames = new ArrayList(body.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) body.get(fieldName);
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

        String vnpSecureHash = VNPayUtils.hmacSHA512(VNPayUtils.vnp_HashSecret, hashData.toString());
        body.put("vnp_SecureHash", vnpSecureHash);

        return body;
    }

    public static Map<String, String> executeQueryRequest(Map<String, String> body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(body);

            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(VNPayUtils.vnp_apiUrl);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new org.apache.http.entity.StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpClient closeable = client; CloseableHttpResponse response = closeable.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                Map<String, String> vpnQueryResult = mapper.readValue(responseBody, Map.class);
                String checksum = vpnQueryResult.get("vnp_SecureHash").toString();
                vpnQueryResult.remove("vnp_SecureHash");

                String calculatedHash = hashAllFields(vpnQueryResult);
                if (!checksum.equals(calculatedHash)) {
                    log.error("Invalid hash: {}", checksum);
                    // Throw or just leave it
                }

                return vpnQueryResult;
            }
        } catch (Exception ex) {
            log.error("Failed to execute query request: {}", ex.getMessage());
            return Map.of("error", "unable to execute query request to vnpay");
        }
    }
}