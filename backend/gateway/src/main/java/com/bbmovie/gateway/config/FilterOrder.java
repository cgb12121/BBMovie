package com.bbmovie.gateway.config;

import org.springframework.core.Ordered;

public class FilterOrder {

    private FilterOrder() {}

    public static final int FIRST = Ordered.HIGHEST_PRECEDENCE;
    public static final int SECOND = Ordered.HIGHEST_PRECEDENCE + 1;
    public static final int THIRD = Ordered.HIGHEST_PRECEDENCE + 2;
}
