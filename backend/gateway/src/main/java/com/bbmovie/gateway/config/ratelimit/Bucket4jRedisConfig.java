package com.bbmovie.gateway.config.ratelimit;

import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Bucket4jRedisConfig {

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<byte[], byte[]> redisConnection(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort) {
        RedisClient redisClient = RedisClient.create(RedisURI.Builder.redis(redisHost, redisPort).build());
        return redisClient.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    public AsyncProxyManager<byte[]> asyncProxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build().asAsync();
    }
}
