package com.bbmovie.gateway.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.Ordered;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterOrderConfig {
    public static final int FIRST = Ordered.HIGHEST_PRECEDENCE;
    public static final int SECOND = Ordered.HIGHEST_PRECEDENCE + 1;
    public static final int THIRD = Ordered.HIGHEST_PRECEDENCE + 2;
}
