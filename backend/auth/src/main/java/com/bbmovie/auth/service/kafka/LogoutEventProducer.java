package com.bbmovie.auth.service.kafka;

import com.bbmovie.auth.config.KafkaTopicConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public LogoutEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a logout event message to the configured Kafka topic.
     *
     * @param key the unique key identifying the logout event to be sent
     */
    public void send(String key) {
        try {
            kafkaTemplate.send(KafkaTopicConfig.LOGOUT_TOPIC, key);
            log.info("Sent logout event to gateway with key for cache: {}", key);
        } catch (Exception e) {
            log.error("Failed to send logout event to gateway with key for cache: {}", key, e);
        }
    }
}
