package com.bbmovie.search.service.nats;

import com.bbmovie.common.dtos.events.SystemHealthEvent;

public interface HealthEventProducer {
    void publishHealthEvent(SystemHealthEvent event);
}
