package com.bbmovie.homepagerecommendations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        properties = {
            "eureka.client.enabled=false",
            "homepage.recommendations.nats.enabled=false"
        })
class HomepageRecommendationsApplicationTests {

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
}
