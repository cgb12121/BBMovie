package com.bbmovie.movieanalyticsservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "movie.analytics.messaging.kafka.enabled=false",
        "movie.analytics.messaging.nats.enabled=false",
        "movie.analytics.clickhouse.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
class MovieAnalyticsServiceApplicationTests {

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }

}
