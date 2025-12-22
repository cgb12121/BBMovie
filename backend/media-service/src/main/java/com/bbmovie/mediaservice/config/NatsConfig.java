package com.bbmovie.mediaservice.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class NatsConfig {

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        return Nats.connect(natsUrl);
    }
}