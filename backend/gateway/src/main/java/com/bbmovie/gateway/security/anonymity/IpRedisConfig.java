package com.bbmovie.gateway.security.anonymity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.nio.charset.StandardCharsets;

@Configuration
public class IpRedisConfig {

    @Bean("ipRedisReactive")
    public ReactiveRedisTemplate<String, Boolean> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        RedisSerializer<Boolean> valueSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(Boolean value) throws SerializationException {
                return (value == null)
                        ? null
                        : value.toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Boolean deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null) return false;
                String str = new String(bytes, StandardCharsets.UTF_8);
                return Boolean.parseBoolean(str);
            }
        };
        RedisSerializationContext<String, Boolean> context = RedisSerializationContext
                .<String, Boolean>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

}