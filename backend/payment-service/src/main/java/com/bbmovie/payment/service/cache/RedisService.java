package com.bbmovie.payment.service.cache;

import com.bbmovie.payment.dto.PaymentCreatedEvent;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.PaymentCacheException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cache(PaymentCreatedEvent event) {
        try {
            log.info("Saving payment record to Redis: {}", event);
            String key = "transaction:" + event.providerTransactionId();
            String value = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(key, value, 15, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment record: {}", e.getMessage());
            throw new PaymentCacheException("Failed to serialize payment record");
        }
    }
}
