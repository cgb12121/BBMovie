package com.bbmovie.mediastreamingservice.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;

@Log4j2
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jose.jwk.endpoint:crash-if-null}")
    private String jwkEndpoint;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("classpath:jwk-dev.json")
    private Resource jwkResource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.addAllowedOrigin("*"); // Allow all origins for streaming (or restrict to your frontend)
                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (Playlists and Segments)
                        // If these are encrypted, they are safe to be public (cannot play without a key)
                        .requestMatchers("/api/stream/segments/**").permitAll()
                        .requestMatchers("/api/stream/*/*.m3u8").permitAll()  // For: /api/stream/{uploadId}/master.m3u8
                        .requestMatchers("/api/stream/*/*/playlist.m3u8").permitAll()
                        
                        // Protected endpoints (Keys)
                        .requestMatchers("/api/stream/keys/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (isDevProfile()) {
            log.warn("[Security] [profile: {}] \n Using hardcoded DEV JWK for JWT validation!", activeProfile);
            return buildDevJwtDecoder();
        } else {
            log.info("[Security] Using remote JWKS from {}", jwkEndpoint);
            return NimbusJwtDecoder.withJwkSetUri(jwkEndpoint).build();
        }
    }

    private JwtDecoder buildDevJwtDecoder() {
        try {
            String jwk = new String(jwkResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JWKSet jwkSet = JWKSet.parse(jwk);

            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

            // Allow RS256 algorithm
            JWSVerificationKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);

            jwtProcessor.setJWSKeySelector(keySelector);

            return new NimbusJwtDecoder(jwtProcessor);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse hardcoded DEV JWK", e);
        }
    }

    private boolean isDevProfile() {
        return activeProfile == null ||
                activeProfile.isBlank() ||
                activeProfile.equalsIgnoreCase("dev") ||
                activeProfile.equalsIgnoreCase("docker") ||
                activeProfile.equalsIgnoreCase("default") ||
                activeProfile.equalsIgnoreCase("local");
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
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
        return converter;
    }
}
