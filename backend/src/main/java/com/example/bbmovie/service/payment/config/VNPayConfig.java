package com.example.bbmovie.service.payment.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class VNPayConfig {
    //Mandatory
    public static String vnp_Version = "2.1.0";
    public static String vnp_command = "pay";

    @Value("${payment.vnpay.TmnCode}")
    public static String vnp_tmn_code;

    @Value("${payment.vnpay.HashSecret}")
    public static String vnp_secure_hash;

    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_return_url = "/vnpay-payment";
    public static String vnp_apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    //This should be config based on users' needs
    // - Nội địa: 01
    // - Quốc tế: 02
    public static String vnp_card_type;
    public static String vnp_txn_ref;
    public static String vnp_txn_desc;
    public static String vnp_ip_addr;
    public static String vnp_create_date;

    //Optional
    public static String vnp_bank_code;
    public static String vnp_locale; //en
    public static String vnp_cancel_url = "";

    public static String hashAllFields(Map<String, Object> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        return hmacSHA512(vnp_secure_hash,sb.toString());
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

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}