package com.bbmovie.fileservice.sercurity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jose.jwk.endpoint}")
    private String jwkEndpoint;

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
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                    jwt -> jwt.jwtDecoder(jwkDecoder())
                    )
            )
            .build();
    }

    public ReactiveJwtDecoder jwkDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkEndpoint).build();
    }
}