package com.bbmovie.fileservice.utils;

import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes and verifies a "privateId" that safely carries a Cloudinary publicId without exposing it directly.
 * Format: base64url(publicId).base64url(HMAC_SHA256(secret, base64url(publicId)))
 */
public final class PrivateIdCodec {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private PrivateIdCodec() { }

    public static String encode(String publicId, String secret) {
        if (!StringUtils.hasText(publicId)) {
            throw new IllegalArgumentException("publicId must not be empty");
        }
        String payload = base64Url(publicId.getBytes(StandardCharsets.UTF_8));
        String signature = base64Url(hmac(payload.getBytes(StandardCharsets.UTF_8), secret));
        return payload + "." + signature;
    }

    /**
     * Validates the token and returns the embedded publicId if valid, otherwise throws IllegalArgumentException.
     */
    public static String decode(String privateId, String secret) {
        if (!StringUtils.hasText(privateId) || !privateId.contains(".")) {
            throw new IllegalArgumentException("Invalid privateId format");
        }
        String[] parts = privateId.split("\\.", 2);
        String payload = parts[0];
        String providedSig = parts[1];

        String expectedSig = base64Url(hmac(payload.getBytes(StandardCharsets.UTF_8), secret));
        if (!constantTimeEquals(providedSig, expectedSig)) {
            throw new IllegalArgumentException("Invalid signature for privateId");
        }

        byte[] decoded = Base64.getUrlDecoder().decode(payload);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private static byte[] hmac(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}


