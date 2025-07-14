package com.bbmovie.fileservice.sercurity;

import com.bbmovie.fileservice.sercurity.jose.TokenBlacklistService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Value("${jose.secret}")
    private String hmacSecret;

    @Value("${jose.jwk.endpoint}")
    private String jwkEndpoint;

    @Value("${jose.jws.endpoint}")
    private String jwsEndpoint;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(auth -> auth
                .pathMatchers("/file/upload").authenticated()
                .pathMatchers("/file/download").authenticated()
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }

    @Bean("jwk")
    public ReactiveJwtDecoder jwkDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkEndpoint).build();
    }

    @Bean("jws")
    public ReactiveJwtDecoder jwsDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwsEndpoint).build();
    }

    @Bean("hmac")
    public ReactiveJwtDecoder hmacDecoder() {
        if (hmacSecret.isEmpty() && !StringUtils.hasText(hmacSecret)) {
            return null;
        }
        SecretKey key = new SecretKeySpec(hmacSecret.getBytes(), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public ReactiveJwtDecoder joseDecoder(
            TokenBlacklistService tokenBlacklistService,
            @Qualifier("jwk") ReactiveJwtDecoder jwkDecoder,
            @Qualifier("jws") ReactiveJwtDecoder jwsDecoder,
            @Qualifier("hmac") ReactiveJwtDecoder hmacDecoder
    ) {
        return token -> {
            String algorithm = extractAlgorithm(token);
            ReactiveJwtDecoder delegate;

            switch (algorithm) {
                case "HMAC" -> delegate = hmacDecoder;
                case "JWS" -> delegate = jwsDecoder;
                default ->  delegate = jwkDecoder;
            }

            return delegate.decode(token)
                    .flatMap(jwt -> {
                        String jti = jwt.getId();
                        if (jti == null) return Mono.error(new JwtException("Missing jti claim"));

                        return tokenBlacklistService.isBlacklisted(jti)
                                .flatMap(blacklisted -> {
                                    if (blacklisted) {
                                        return Mono.error(new JwtException("Unauthorize access."));
                                    }
                                    return Mono.just(jwt);
                                });
                    });
        };
    }

    private String extractAlgorithm(String token) {
        try {
            String header = token.split("\\.")[0];
            String json = new String(Base64.getUrlDecoder().decode(header), StandardCharsets.UTF_8);
            Map<String, Object> map = new ObjectMapper().readValue(json, new TypeReference<>() {});
            return (String) map.get("alg");
        } catch (Exception e) {
            throw new JwtException("Failed to extract JWT algorithm", e);
        }
    }
}
