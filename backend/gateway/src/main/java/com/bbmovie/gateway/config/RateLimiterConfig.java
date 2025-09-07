package com.bbmovie.gateway.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RateLimiterConfig {
    public static final String DEFAULT_RATE_LIMITER_KEY = "rateLimitKey";
}
