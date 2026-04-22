package com.bbmovie.watchhistory.service;

import com.bbmovie.watchhistory.config.WatchTrackingProperties;
import com.bbmovie.watchhistory.dto.PlaybackAnalyticsEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.PublishOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NatsPlaybackPublisher {

    private final ObjectProvider<JetStream> jetStream;
    private final ObjectMapper objectMapper;
    private final WatchTrackingProperties properties;

    public boolean isNatsEnabled() {
        return properties.getNats().isEnabled();
    }

    /**
     * @return true if message was accepted by JetStream
     */
    public boolean tryPublish(PlaybackAnalyticsEvent event) {
        if (!properties.getNats().isEnabled()) {
            return false;
        }
        JetStream js = jetStream.getIfAvailable();
        if (js == null) {
            log.warn("NATS enabled but JetStream bean is missing");
            return false;
        }
        try {
            byte[] data = objectMapper.writeValueAsBytes(event);
            PublishOptions opts = PublishOptions.builder().messageId(event.eventId()).build();
            js.publish(properties.getNats().getSubject(), data, opts);
            return true;
        } catch (Exception e) {
            log.warn("NATS publish failed: {}", e.getMessage());
            return false;
        }
    }
}
