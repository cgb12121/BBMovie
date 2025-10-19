package com.bbmovie.gateway.config.ratelimit;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
class RedisConnectionHealthEvent extends ApplicationEvent {
    private final boolean healthy;

    public RedisConnectionHealthEvent(Object source, boolean healthy) {
        super(source);
        this.healthy = healthy;
    }
}