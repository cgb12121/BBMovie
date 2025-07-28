package com.bbmovie.auth.service.kafka;

import com.bbmovie.auth.config.KafkaTopicConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ABACEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public ABACEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String key) {
        try {
            kafkaTemplate.send(KafkaTopicConfig.ABAC_TOPIC, key);
        } catch (Exception e) {
            log.error("send ABAC event error", e);
        }
    }
}
