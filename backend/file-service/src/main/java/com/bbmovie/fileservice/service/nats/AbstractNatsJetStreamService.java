package com.bbmovie.fileservice.service.nats;

import com.bbmovie.fileservice.dto.event.NatsConnectionEvent;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public abstract class AbstractNatsJetStreamService {

    private final AtomicReference<JetStream> jetStreamRef = new AtomicReference<>();

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            String serviceName = this.getClass().getSimpleName();
            log.info("NATS connected, initializing JetStream context for: {}", serviceName);
            try {
                jetStreamRef.set(event.connection().jetStream());
            } catch (IOException e) {
                log.error("Failed to create JetStream context for {} after NATS connection was established.", serviceName, e);
            }
        }
    }

    protected JetStream getJetStream() {
        return jetStreamRef.get();
    }
}