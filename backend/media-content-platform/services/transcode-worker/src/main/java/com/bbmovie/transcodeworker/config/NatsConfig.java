package com.bbmovie.transcodeworker.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration class for NATS messaging system client.
 * Sets up the NATS connection for message publishing and consuming operations.
 */
@Configuration
public class NatsConfig {

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    /**
     * Creates and returns a NATS connection bean instance.
     *
     * @return Connection instance configured with the specified NATS URL
     * @throws IOException if there's an issue connecting to NATS
     * @throws InterruptedException if the connection process is interrupted
     */
    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        return Nats.connect(natsUrl);
    }
}
