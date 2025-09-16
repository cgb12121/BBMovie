package com.bbmovie.payment.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Properties;

@Configuration
public class RedisConfig {

    @PostConstruct
    public void enableKeyspaceNotifications(RedisConnectionFactory connectionFactory) {
        RedisConnection conn = connectionFactory.getConnection();

        String targetCmd = "notify-keyspace-events";
        RedisServerCommands serverCommands = conn.serverCommands();
        Properties cmd = serverCommands.getConfig(targetCmd);

        String current = null;
        if (cmd != null && cmd.getProperty(targetCmd) != null) {
            current = (String) cmd.get(targetCmd);
        }
        if (current == null || !current.contains("Ex")) {
            serverCommands.setConfig(targetCmd, (current == null ? "" : current) + "Ex");
        }
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
