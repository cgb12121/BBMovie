package com.example.bbmovie.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtKeyGeneratorUtil {
    public static void main(String[] args) {
        byte[] key = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);

        String base64Key = Base64.getEncoder().encodeToString(key);
        System.out.println("Generated 256-bit JWT key (Base64): " + base64Key);
    }
}