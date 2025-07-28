package com.bbmovie.auth.service.kafka;

import com.bbmovie.auth.config.KafkaTopicConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class LogoutEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public LogoutEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String key) {
        try {
            kafkaTemplate.send(KafkaTopicConfig.LOGOUT_TOPIC, key);
            log.info("Sent logout event to gateway with key for cache: {}", key);
        } catch (Exception e) {
            log.error("Failed to send logout event to gateway with key for cache: {}", key, e);
        }
    }
}
