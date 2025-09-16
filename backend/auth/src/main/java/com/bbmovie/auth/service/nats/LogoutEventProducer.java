package com.bbmovie.auth.service.nats;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.nats.client.Connection;
import io.nats.client.JetStream;

/**
 * This class is responsible for producing logout event messages and sending them to a Kafka topic.
 * It is used to notify other systems or services about logout actions within the application.
 * <p>
 * The Kafka topic for logout events is pre-configured in the application using {@link KafkaTopicConfig}.
 * <p>
 * The class relies on Spring's {@link KafkaTemplate} to handle message production and
 * uses logging to provide feedback on the success or failure of message delivery.
 * <p>
 * Dependencies:
 * - {@link KafkaTemplate}: Used for sending messages to Kafka.
 * - {@link KafkaTopicConfig}: Provides configuration for the Kafka topic used by this producer.
 */
@Log4j2
@Service
public class LogoutEventProducer {

    private final JetStream jetStream;

    @Autowired
    public LogoutEventProducer(Connection nats) throws java.io.IOException {
        this.jetStream = nats.jetStream();
    }

    /**
     * Sends a logout event message to the configured Kafka topic.
     *
     * @param key the unique key identifying the logout event to be sent
     */
    public void send(String key) {
        try {
            byte[] data = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            jetStream.publish("auth.logout", data);
            log.info("Published logout event to auth.logout with key for cache: {}", key);
        } catch (Exception e) {
            log.error("Failed to publish logout event with key: {}", key, e);
        }
    }
}
