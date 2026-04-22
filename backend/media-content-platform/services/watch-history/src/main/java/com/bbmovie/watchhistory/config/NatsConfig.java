package com.bbmovie.watchhistory.config;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "watch-history.tracking.nats.enabled", havingValue = "true")
public class NatsConfig {

    @Bean(destroyMethod = "close")
    public Connection natsConnection(WatchTrackingProperties properties) throws IOException, InterruptedException {
        String url = properties.getNats().getUrl();
        log.info("Connecting to NATS at {}", url);
        return Nats.connect(url);
    }

    @Bean
    public JetStream jetStream(Connection connection) throws IOException, JetStreamApiException {
        return connection.jetStream();
    }
}
