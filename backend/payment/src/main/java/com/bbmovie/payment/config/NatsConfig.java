package com.bbmovie.payment.config;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
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
        String streamName = "PAYMENTS";

        // --- Stream ---
        try {
            StreamInfo stream = jsm.getStreamInfo(streamName);
            log.info("Stream already exists {}", stream);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(streamName)
                        .subjects("payments.*")
                        .storageType(StorageType.File)
                        .build();
                jsm.addStream(streamConfig);
                log.info("Created stream: {}", streamName);
            } else {
                throw e;
            }
        }

        // --- Consumer ---
//        String consumerName = "email-service";
//        try {
//            ConsumerInfo consumerInfo = jsm.getConsumerInfo(streamName, consumerName);
//            log.info("Consumer already exists {}", consumerInfo);
//        } catch (JetStreamApiException e) {
//            if (e.getErrorCode() == 404) {
//                ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
//                        .durable(consumerName)
//                        .filterSubject("payments.success")
//                        .ackPolicy(AckPolicy.Explicit)
//                        .build();
//                jsm.addOrUpdateConsumer(streamName, consumerConfig);
//                log.info("Created consumer: {}", consumerName);
//            } else {
//                throw e;
//            }
//        }
    }
}
