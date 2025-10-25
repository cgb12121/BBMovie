package com.bbmovie.ai_assistant_service.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.example.common.entity.JoseConstraint;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;

import static com.example.common.entity.JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER;
import static com.example.common.entity.JoseConstraint.JosePayload.ROLE;

@ConditionalOnProperty(
        name = "spring.security.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String JWKS_URL = "http://localhost:8080/.well-known/jwks.json";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/admin/**").hasAnyRole("ROLE_ADMIN", "ADMIN")
                .pathMatchers("/experimental/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder())
                    .jwtAuthenticationConverter(reactiveJwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    public NimbusReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(JWKS_URL).build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            List<GrantedAuthority> authorities = new ArrayList<>();

            String role = jwt.getClaimAsString(ROLE);
            if (role != null && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            String subscriptionTier = jwt.getClaimAsString(SUBSCRIPTION_TIER);
            if (subscriptionTier != null && !subscriptionTier.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("TIER_" + subscriptionTier));
            }

            if (authorities.isEmpty()) {
                 authorities.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
            }
            return authorities;
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
