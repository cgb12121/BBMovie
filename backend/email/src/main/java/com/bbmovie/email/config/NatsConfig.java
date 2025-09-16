package com.bbmovie.email.config;

import io.nats.client.*;
import io.nats.client.api.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Log4j2
@Configuration
public class NatsConfig {

    @Bean(destroyMethod = "close")
    public Connection natsConnection() throws IOException, InterruptedException {
        return Nats.connect("nats://localhost:4222");   // Connect to NATS server (JetStream enabled)
    }

    @Bean
    public JetStreamManagement jetStreamManagement(Connection nc) throws IOException {
        return nc.jetStreamManagement();
    }

    @PostConstruct
    public void setup(JetStreamManagement jsm) throws IOException, JetStreamApiException {
        // Ensure stream exists with subjects we care about
        String streamName = "PAYMENTS";
        try {
            jsm.getStreamInfo(streamName);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                StreamConfiguration cfg = StreamConfiguration.builder()
                        .name(streamName)
                        .subjects("payments.success", "payments.subscription.*", "payment.report.daily")
                        .build();
                jsm.addStream(cfg);
                log.info("Created stream {}", streamName);
            } else {
                throw e;
            }
        }

        // Durable consumer for payments.success emails
        String consumerName = "email-service";
        try {
            jsm.getConsumerInfo(streamName, consumerName);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                        .durable(consumerName)
                        .filterSubject("payments.success")
                        .ackPolicy(AckPolicy.Explicit)
                        .build();
                jsm.addOrUpdateConsumer(streamName, consumerConfig);
                log.info("Created consumer: {}", consumerName);
            } else {
                throw e;
            }
        }

        // Durable consumer for subscription events
        String subsConsumer = "email-subscription-service";
        try {
            jsm.getConsumerInfo(streamName, subsConsumer);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                        .durable(subsConsumer)
                        .filterSubject("payments.subscription.*")
                        .ackPolicy(AckPolicy.Explicit)
                        .build();
                jsm.addOrUpdateConsumer(streamName, consumerConfig);
                log.info("Created consumer: {}", subsConsumer);
            } else {
                throw e;
            }
        }
    }
}