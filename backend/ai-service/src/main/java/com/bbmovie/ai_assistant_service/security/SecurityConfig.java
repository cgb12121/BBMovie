package com.bbmovie.ai_assistant_service.security;

import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(SecurityConfig.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String JWKS_URL;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("classpath:jwk-dev.json")
    private Resource jwkResource;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/admin/**").hasAnyRole("ROLE_ADMIN", "ADMIN")
                .pathMatchers("/api/v1/**").authenticated() // Allow access to the new API
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwtDecoder(reactiveJwtDecoder())
                        .jwtAuthenticationConverter(reactiveJwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        if (isDevProfile()) {
            log.error("[Security] [profile: {}] \n Using hardcoded DEV JWK for JWT validation!", activeProfile);
            return buildDevJwtDecoder();
        } else {
            log.info("[Security] Using remote JWKS from {}", JWKS_URL);
            return NimbusReactiveJwtDecoder.withJwkSetUri(JWKS_URL).build();
        }
    }

    private ReactiveJwtDecoder buildDevJwtDecoder() {
        JWKSet jwkSet;
        try {
            String jwk = new String(jwkResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            jwkSet = JWKSet.parse(jwk);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse hardcoded DEV JWK", e);
        }

        Function<SignedJWT, Flux<JWK>> jwkSource = signedJwt -> Flux.fromIterable(jwkSet.getKeys());

        return NimbusReactiveJwtDecoder.withJwkSource(jwkSource).build();
    }

    //TODO: remove this when we have a proper profile system
    private boolean isDevProfile() {
        return activeProfile == null ||
                activeProfile.isBlank() ||
                activeProfile.equalsIgnoreCase("dev") ||
                activeProfile.equalsIgnoreCase("docker") ||
                activeProfile.equalsIgnoreCase("default") ||
                activeProfile.equalsIgnoreCase("local");
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
