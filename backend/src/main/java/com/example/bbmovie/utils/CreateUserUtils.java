package com.example.bbmovie.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Random;

public class CreateUserUtils {
    private static final Random random = new SecureRandom();

    public static String generateRandomPasswordFoForOauth2() {
        return RandomStringUtils.random(20, 0, 0, true,true, null, random);
    }

    public static String generateRandomUsername() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random);
    }

    public static String generateRandomEmail() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random) + "@bbmovie.com";
    }

    public static String generateRandomAvatarUrl() {
        int from1to1084cuzFreeApiHas1084img = new Random().nextInt(1084) + 1;
        return "https://picsum.photos/images#" + from1to1084cuzFreeApiHas1084img + "/200/200";
    }
}
