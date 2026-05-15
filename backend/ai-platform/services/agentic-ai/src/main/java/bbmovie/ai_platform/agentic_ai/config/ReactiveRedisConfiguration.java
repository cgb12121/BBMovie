package bbmovie.ai_platform.agentic_ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class ReactiveRedisConfiguration {

     @Bean
     public RedisSerializer<Object> springSessionDefaultRedisSerializer(ObjectMapper objectMapper) {
          return new GenericJacksonJsonRedisSerializer(objectMapper); // JSON serialization
     }

     @Primary
     @Bean("rRedisTemplate")
     public ReactiveRedisTemplate<String, String> rRedisTemplate(ReactiveRedisConnectionFactory factory) {
          StringRedisSerializer StringRedisSerializer = new StringRedisSerializer();
          RedisSerializationContext<String, String> context = RedisSerializationContext
                    .<String, String>newSerializationContext(StringRedisSerializer)
                    .value(StringRedisSerializer)
                    .value(StringRedisSerializer)
                    .hashKey(StringRedisSerializer)
                    .hashValue(StringRedisSerializer)
                    .build();

          return new ReactiveRedisTemplate<>(factory, context);
     }
}
