package com.bbmovie.search.service.nats;

import com.bbmovie.search.config.NatsConfig;
import com.bbmovie.search.dto.event.NatsConnectionEvent;
import com.example.common.dtos.nats.VideoMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Dispatcher;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class VideoEventConsumer {

    private final Connection nats;
    private final ObjectMapper objectMapper;

    @Autowired
    public VideoEventConsumer(NatsConfig.NatsConnectionFactory natsConnectionFactory, ObjectMapper objectMapper) {
        this.nats = natsConnectionFactory.getConnection();
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected/reconnected, (re)subscribing…");
            setupVideoEventSubscriptions();
        }
    }

    private void setupVideoEventSubscriptions() {
        Dispatcher dispatcher = this.nats.createDispatcher(msg -> {
            try {
                VideoMetadata videoMetadata = objectMapper.readValue(msg.getData(), VideoMetadata.class);
                msg.ack();
                handle(videoMetadata);
            } catch (Exception e) {
                log.error("Error while processing video metadata event", e);
                msg.nak();
            }
        });

        dispatcher.subscribe("video.metadata");
        log.info("Subscribed to video.metadata events");
    }

    private void handle(VideoMetadata videoMetadata) {
        log.info("Received video metadata event: {}", videoMetadata);
        // Here you would typically update your search index with the new video metadata
    }
}
