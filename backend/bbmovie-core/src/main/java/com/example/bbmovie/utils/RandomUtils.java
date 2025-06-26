package com.example.bbmovie.utils;

import java.security.SecureRandom;
import java.util.Random;

public class RandomUtils {
    private RandomUtils() {}

    public static final Random random = new Random();

    public static final Random secureRandom = new SecureRandom();
}
