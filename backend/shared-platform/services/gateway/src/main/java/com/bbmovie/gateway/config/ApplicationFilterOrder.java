package com.bbmovie.gateway.config;

import org.springframework.core.Ordered;

public class ApplicationFilterOrder {
    public static final int REQUEST_LOGGING_FILTER = Ordered.HIGHEST_PRECEDENCE;
    public static final int RATE_LIMITING_FILTER = Ordered.HIGHEST_PRECEDENCE + 25;
    public static final int ANONYMITY_CHECK_FILTER = Ordered.HIGHEST_PRECEDENCE + 50;
    public static final int AUTHENTICATION_FILTER = Ordered.HIGHEST_PRECEDENCE + 75;
}