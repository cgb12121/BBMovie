package com.bbmovie.payment.service.zalopay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"squid:S1118", "unused"})
public final class ZaloHmacUtil {
    public static final String HMACMD5 = "HmacMD5";
    public static final String HMACSHA1 = "HmacSHA1";
    public static final String HMACSHA256 = "HmacSHA256";
    public static final String HMACSHA512 = "HmacSHA512";

    private static byte[] hmacEncode(String algorithm, String key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static String hmacHexStringEncode(String algorithm, String key, String data) {
        byte[] result = hmacEncode(algorithm, key, data);
        return result != null ? ZaloHexStringUtil.byteArrayToHexString(result) : null;
    }
}
