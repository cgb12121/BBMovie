package com.bbmovie.personalizationrecommendation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "personalization.qdrant.enabled=false"
})
class PersonalizationRecommendationApplicationTests {

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }

}
