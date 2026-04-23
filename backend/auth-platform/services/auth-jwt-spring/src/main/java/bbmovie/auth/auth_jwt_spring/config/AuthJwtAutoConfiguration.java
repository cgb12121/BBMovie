package bbmovie.auth.auth_jwt_spring.config;

import bbmovie.auth.auth_jwt_core.blacklist.InMemoryTokenBlacklistChecker;
import bbmovie.auth.auth_jwt_core.blacklist.RedisTokenBlacklistChecker;
import bbmovie.auth.auth_jwt_core.blacklist.TokenBlacklistChecker;
import bbmovie.auth.auth_jwt_core.jwks.InMemoryJwksCache;
import bbmovie.auth.auth_jwt_core.jwks.JwksCache;
import bbmovie.auth.auth_jwt_core.jwks.RedisJwksCache;
import bbmovie.auth.auth_jwt_spring.support.AuthJwtToolkit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(AuthJwtSpringProperties.class)
public class AuthJwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public JwksCache redisJwksCache(
            StringRedisTemplate redisTemplate,
            AuthJwtSpringProperties properties
    ) {
        return new RedisJwksCache(redisTemplate, properties.getJwksCacheRedisKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwksCache inMemoryJwksCache() {
        return new InMemoryJwksCache();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public TokenBlacklistChecker redisTokenBlacklistChecker(
            StringRedisTemplate redisTemplate,
            AuthJwtSpringProperties properties
    ) {
        return new RedisTokenBlacklistChecker(
                redisTemplate,
                properties.getBlacklistSidRedisPrefix(),
                properties.getBlacklistJtiRedisPrefix()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenBlacklistChecker inMemoryTokenBlacklistChecker() {
        return new InMemoryTokenBlacklistChecker();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthJwtToolkit authJwtToolkit(AuthJwtSpringProperties properties, ResourceLoader resourceLoader) {
        return new AuthJwtToolkit(properties, resourceLoader);
    }
}
