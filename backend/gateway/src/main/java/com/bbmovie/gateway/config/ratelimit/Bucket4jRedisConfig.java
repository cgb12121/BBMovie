package com.bbmovie.gateway.config.ratelimit;

import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.ConnectionDeactivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Log4j2
@Configuration
public class Bucket4jRedisConfig {

    @Bean
    public StatefulRedisConnection<byte[], byte[]> redisConnection(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort) {
        ClientResources clientResources = DefaultClientResources.builder()
                .build();
        RedisURI redisURI = RedisURI.Builder.redis(redisHost, redisPort)
                .withTimeout(Duration.ofSeconds(10))
                .build();

        RedisClient redisClient = RedisClient.create(clientResources, redisURI);

        redisClient.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .suspendReconnectOnProtocolFailure(false) // Allow retries on protocol failures
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .keepAlive(true)
                        .build())
                .build());

        EventBus eventBus = clientResources.eventBus();
        Flux.from(eventBus.get())
                .subscribe(event -> {
                    if (event instanceof ConnectionActivatedEvent) {
                        log.info("Redis connection activated: {}", event);
                    } else if (event instanceof DisconnectedEvent) {
                        log.warn("Redis connection disconnected: {}", event);
                    } else if (event instanceof ReconnectFailedEvent) {
                        log.error("Redis reconnect failed: {}", event);
                    } else if (event instanceof ConnectionDeactivatedEvent) {
                        log.info("Redis connection deactivated: {}", event);
                    }
                });

        // Create and connect
        return redisClient.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    public AsyncProxyManager<byte[]> asyncProxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection)
                .build()
                .asAsync();
    }
}
