package com.bbmovie.payment.config;

import com.bbmovie.payment.service.cache.RedisKeyExpirationListener;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Properties;

@Configuration
public class RedisKeyExpirationConfig {

    private final RedisConnectionFactory connectionFactory;

    @Autowired
    public RedisKeyExpirationConfig(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisKeyExpirationListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new PatternTopic("__keyevent@0__:expired")); // listen to expired events
        return container;
    }

    //this config allows redis to publish event on key expiration
    //NOTE: some redis server might disable or disallow this config => must config via redis.conf or cloud console
    @PostConstruct
    public void enableKeyspaceNotifications() {
        try (RedisConnection conn = connectionFactory.getConnection()) {
            RedisServerCommands serverCommands = conn.serverCommands();
            String targetCmd = "notify-keyspace-events";

            Properties config = serverCommands.getConfig(targetCmd);
            assert config != null;
            String current = config.getProperty(targetCmd, "");

            if (!current.contains("Ex")) {
                String newValue = current + "Ex";
                serverCommands.setConfig(targetCmd, newValue);
            }
        }
    }

    @Bean
    public KeyExpirationListener keyExpirationListener() {
        return new KeyExpirationListener();
    }

    @Log4j2
    public static class KeyExpirationListener implements MessageListener {
        @Override
        public void onMessage(Message message, byte[] pattern) {
            String expiredKey = message.toString();
            log.info("Redis key expired: {}", expiredKey);
        }
    }
}
