package com.bbmovie.auth.security.jose.nimbus;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UnsupportedOAuth2Provider;
import com.bbmovie.auth.exception.UnsupportedPrincipalType;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component("nimbusJws")
public class NimbusJws implements JoseProviderStrategy {

    private final int jwtAccessTokenExpirationInMs;
    private final int jwtRefreshTokenExpirationInMs;
    private final RSAKey activePrivateKey;
    private final List<RSAKey> publicKeys;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final List<OAuth2UserInfoStrategy> strategies;
    private static final String JWT_BLACKLIST_PREFIX = "jose-blacklist:";

    public NimbusJws(
            @Value("${app.jose.expiration.access-token}") int jwtAccessTokenExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jwtRefreshTokenExpirationInMs,
            @Qualifier("activePrivateKey") RSAKey activePrivateKey,
            List<RSAKey> publicKeys,
            RedisTemplate<Object, Object> redisTemplate,
            List<OAuth2UserInfoStrategy> strategies
    ) {
        this.jwtAccessTokenExpirationInMs = jwtAccessTokenExpirationInMs;
        this.jwtRefreshTokenExpirationInMs = jwtRefreshTokenExpirationInMs;
        this.activePrivateKey = activePrivateKey;
        this.publicKeys = publicKeys;
        this.redisTemplate = redisTemplate;
        this.strategies = strategies;
    }

    @Override
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtAccessTokenExpirationInMs);
    }

    @Override
    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jwtRefreshTokenExpirationInMs);
    }

    private String generateToken(Authentication authentication, int expirationInMs) {
        try {
            String username = getUsernameFromAuthentication(authentication);
            String role = getRoleFromAuthentication(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer("bbmovie-core")
                    .subject(username)
                    .claim("role", role)
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .jwtID(UUID.randomUUID().toString())
                    .claim("sid", UUID.randomUUID().toString())
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .keyID(activePrivateKey.getKeyID())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new RSASSASigner(activePrivateKey.toRSAPrivateKey()));
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Token generation error: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate JWT", e);
        }
    }

    private String getUsernameFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof DefaultOAuth2User oauth2User) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            String provider = token.getAuthorizedClientRegistrationId();
            Map<String, Object> attributes = oauth2User.getAttributes();

            OAuth2UserInfoStrategy strategy = getStrategyForProvider(provider);
            return strategy.getUsername(attributes);
        }
        throw new UnsupportedPrincipalType(
                "Unsupported principal type: " + principal.getClass().getName()
        );
    }

    private OAuth2UserInfoStrategy getStrategyForProvider(String provider) {
        return strategies.stream()
                .filter(s -> s.getAuthProvider().name().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOAuth2Provider("Unsupported provider: " + provider));
    }

    private String getRoleFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return "ROLE_" + user.getRole().name();
        } else if (principal instanceof UserDetails userDetails) {
            return userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");
        } else if (principal instanceof DefaultOAuth2User) {
            return "ROLE_USER";
        }
        throw new UnsupportedPrincipalType(
                "Unsupported principal type: " + principal.getClass().getName()
        );
    }

    @Override
    public boolean validateToken(String token) {
        Optional<SignedJWT> verifiedJwt = resolveAndVerify(token);
        if (verifiedJwt.isEmpty()) {
            return false;
        }
        try {
            return verifiedJwt.get().getJWTClaimsSet().getExpirationTime().after(new Date());
        } catch (ParseException e) {
            log.error("Failed to parse token expiration: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getSubject();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid JWK username", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Failed to parse username from JWK token"));
    }

    @Override
    public List<String> getRolesFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        String role = (String) jwt.getJWTClaimsSet().getClaim("role");
                        return List.of(role);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid JWK"));
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getExpirationTime();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid JWK"));
    }

    @Override
    public void invalidateAccessTokenByEmailAndDevice(String email, String deviceName) {
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.opsForValue().set(key, "true", 15, TimeUnit.MINUTES);
    }

    @Override
    public boolean isAccessTokenBlacklistedForEmailAndDevice(String email, String deviceName) {
        if (email == null || deviceName == null) {
            return false;
        }
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        return redisTemplate != null && redisTemplate.hasKey(key);
    }

    @Override
    public void removeBlacklistedAccessTokenOfEmailAndDevice(String email, String deviceName) {
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.delete(key);
    }

    private Optional<SignedJWT> resolveAndVerify(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Invalid token format (null, blank, or missing 3 JWT parts): '{}'", token);
            return Optional.empty();
        }

        try {
            SignedJWT jwt = SignedJWT.parse(token);
            String kid = jwt.getHeader().getKeyID();
            if (kid == null) {
                log.error("Token missing key ID (kid)");
                return Optional.empty();
            }

            for (RSAKey key : publicKeys) {
                if (key.getKeyID().equals(kid)) {
                    if (jwt.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                        log.info("Token verified with kid: {}", kid);
                        return Optional.of(jwt);
                    } else {
                        log.error("Token verification failed for kid: {}", kid);
                        return Optional.empty();
                    }
                }
            }
            log.error("No matching key found for kid: {}", kid);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to verify token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}