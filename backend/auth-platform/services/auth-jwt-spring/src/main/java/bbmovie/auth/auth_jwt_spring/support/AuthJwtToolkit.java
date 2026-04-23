package bbmovie.auth.auth_jwt_spring.support;

import bbmovie.auth.auth_jwt_spring.config.AuthJwtMode;
import bbmovie.auth.auth_jwt_spring.config.AuthJwtSpringProperties;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class AuthJwtToolkit {
    private final AuthJwtSpringProperties properties;
    private final ResourceLoader resourceLoader;

    public AuthJwtToolkit(AuthJwtSpringProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public JwtDecoder servletJwtDecoder() {
        if (shouldUseDevJwk()) {
            JWKSet jwkSet = readDevJwkSet();
            JWK firstKey = jwkSet.getKeys().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No key found in dev JWK set"));
            try {
                return NimbusJwtDecoder.withPublicKey(firstKey.toRSAKey().toRSAPublicKey()).build();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create servlet JwtDecoder from dev JWK", ex);
            }
        }
        if (properties.getRemoteJwksUri() == null || properties.getRemoteJwksUri().isBlank()) {
            throw new IllegalStateException("auth.jwt.remote-jwks-uri is required for remote JWKS mode");
        }
        return NimbusJwtDecoder.withJwkSetUri(properties.getRemoteJwksUri()).build();
    }

    public ReactiveJwtDecoder reactiveJwtDecoder() {
        if (shouldUseDevJwk()) {
            JWKSet jwkSet = readDevJwkSet();
            Function<SignedJWT, Flux<JWK>> jwkSource = signedJwt -> Flux.fromIterable(jwkSet.getKeys());
            return NimbusReactiveJwtDecoder.withJwkSource(jwkSource).build();
        }
        if (properties.getRemoteJwksUri() == null || properties.getRemoteJwksUri().isBlank()) {
            throw new IllegalStateException("auth.jwt.remote-jwks-uri is required for remote JWKS mode");
        }
        return NimbusReactiveJwtDecoder.withJwkSetUri(properties.getRemoteJwksUri()).build();
    }

    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    public ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter());
    }

    private List<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        String role = jwt.getClaimAsString(properties.getRoleClaim());
        if (role != null && !role.isBlank()) {
            authorities.add(new SimpleGrantedAuthority(properties.getRolePrefix() + role));
        }

        String subscriptionTier = jwt.getClaimAsString(properties.getSubscriptionTierClaim());
        if (subscriptionTier != null && !subscriptionTier.isBlank()) {
            authorities.add(new SimpleGrantedAuthority(properties.getTierPrefix() + subscriptionTier));
        }

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority(properties.getDefaultAuthority()));
        }
        return authorities;
    }

    private boolean shouldUseDevJwk() {
        if (properties.getMode() == AuthJwtMode.DEV_STATIC_JWK) {
            return true;
        }
        if (properties.getMode() == AuthJwtMode.REMOTE_JWKS) {
            return false;
        }
        String profile = properties.getActiveProfile();
        if (profile == null) {
            return true;
        }
        String normalized = profile.toLowerCase(Locale.ROOT);
        return normalized.isBlank()
                || normalized.equals("dev")
                || normalized.equals("docker")
                || normalized.equals("default")
                || normalized.equals("local");
    }

    private JWKSet readDevJwkSet() {
        try {
            Resource jwkResource = resourceLoader.getResource(properties.getDevJwkPath());
            String jwk = new String(jwkResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return JWKSet.parse(jwk);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read dev JWK from " + properties.getDevJwkPath(), ex);
        }
    }
}
