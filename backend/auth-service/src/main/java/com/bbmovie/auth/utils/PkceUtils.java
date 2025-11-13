package com.bbmovie.auth.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtils {

    private PkceUtils() {}

    private static final SecureRandom secureRandom = new SecureRandom();

    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

    public static String generateCodeVerifier() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        StringBuilder verifier = new StringBuilder();
        for (byte b : randomBytes) {
            verifier.append(ALLOWED_CHARS.charAt(Math.abs(b % ALLOWED_CHARS.length())));
        }
        return verifier.toString();
    }

    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
