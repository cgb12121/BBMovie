package com.bbmovie.payment.utils;

import java.security.SecureRandom;
import java.util.Random;

public class RandomUtil {

    private RandomUtil() {
    }
     
     private static final Random RANDOM = new SecureRandom();

     public static String getRandomNumber(int len) {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
