package com.example.bbmovieuploadfile.sercurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    private static final String JWK_ENDPOINT_URI = "http://localhost:8080/.well-known/jwks.json";

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

    @Bean
    public ReactiveJwtDecoder jwtDecoder(TokenBlacklistService tokenBlacklistService) {
        NimbusReactiveJwtDecoder delegate = NimbusReactiveJwtDecoder.withJwkSetUri(JWK_ENDPOINT_URI).build();

        return token -> delegate.decode(token)
                .flatMap(jwt -> {
                    String jti = jwt.getId();
                    if (jti == null) return Mono.error(new JwtException("Missing jti claim"));

                    return tokenBlacklistService.isBlacklisted(jti)
                            .flatMap(blacklisted -> {
                                if (blacklisted) {
                                    return Mono.error(new JwtException("Token is blacklisted"));
                                }
                                return Mono.just(jwt);
                            });
                });
    }

}
