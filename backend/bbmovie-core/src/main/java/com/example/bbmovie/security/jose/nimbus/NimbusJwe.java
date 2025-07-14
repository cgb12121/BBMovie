package com.example.bbmovie.security.jose.nimbus;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.UnsupportedOAuth2Provider;
import com.example.bbmovie.exception.UnsupportedPrincipalType;
import com.example.bbmovie.security.jose.JoseProviderStrategy;
import com.example.bbmovie.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component("nimbusJwe")
public class NimbusJwe implements JoseProviderStrategy {

    private final int jwtAccessTokenExpirationInMs;
    private final int jwtRefreshTokenExpirationInMs;
    private final RSAKey activePrivateKey;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final List<OAuth2UserInfoStrategy> strategies;

    private static final String JWT_BLACKLIST_PREFIX = "jose-blacklist:";

    public NimbusJwe(
            @Value("${app.jose.expiration.access-token}") int jwtAccessTokenExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jwtRefreshTokenExpirationInMs,
            @Qualifier("activePrivateKey") RSAKey activePrivateKey,
            RedisTemplate<Object, Object> redisTemplate,
            List<OAuth2UserInfoStrategy> strategies
    ) {
        this.jwtAccessTokenExpirationInMs = jwtAccessTokenExpirationInMs;
        this.jwtRefreshTokenExpirationInMs = jwtRefreshTokenExpirationInMs;
        this.activePrivateKey = activePrivateKey;
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
            String username = extractUsername(authentication);
            String role = extractRole(authentication);
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

            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                    .contentType("JWT")
                    .keyID(activePrivateKey.getKeyID())
                    .build();

            EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

            RSAKey publicKey = activePrivateKey.toPublicJWK();
            JWEEncrypter encrypter = new RSAEncrypter(publicKey);

            encryptedJWT.encrypt(encrypter);

            return encryptedJWT.serialize();
        } catch (Exception e) {
            log.error("Token encryption error: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate JWE token", e);
        }
    }

    private Optional<EncryptedJWT> resolveAndDecrypt(String token) {
        if(token == null || token.isBlank()) return Optional.empty();

        if (token.split("\\.").length != 5) {
            log.warn("Invalid JWE format (expected 5 parts): '{}'", token);
            return Optional.empty();
        }

        try {
            EncryptedJWT jwt = EncryptedJWT.parse(token);
            RSADecrypter decrypter = new RSADecrypter(activePrivateKey.toRSAPrivateKey());
            jwt.decrypt(decrypter);
            log.debug("Decrypted JWT: {}", jwt.serialize());
            return Optional.of(jwt);
        } catch (Exception e) {
            log.error("Failed to decrypt JWE token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean validateToken(String token) {
        return resolveAndDecrypt(token)
                .map(jwt -> {
                    try {
                        Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
                        boolean expired = expirationTime.before(new Date());
                        if (expired) {
                            log.warn("Expired JWT token: {}", token);
                            return false;
                        }
                        return true;
                    } catch (ParseException e) {
                        log.error("Error parsing expiration: {}", e.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }

    @Override
    public String getUsernameFromToken(String token) {
        return resolveAndDecrypt(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getSubject();
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Invalid JWT subject", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Failed to resolve username from token"));
    }

    @Override
    public List<String> getRolesFromToken(String token) {
        return resolveAndDecrypt(token)
                .map(jwt -> {
                    try {
                        String role = (String) jwt.getJWTClaimsSet().getClaim("role");
                        return List.of(role);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Invalid JWT role", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Failed to resolve username from token"));
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        return resolveAndDecrypt(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getExpirationTime();
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Invalid JWT expiration", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Token invalid or unverified"));
    }

    @Override
    public void invalidateAccessTokenByEmailAndDevice(String email, String deviceName) {
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.opsForValue().set(key, "true", 15, TimeUnit.MINUTES);
    }

    @Override
    public boolean isAccessTokenBlacklistedForEmailAndDevice(String email, String deviceName) {
        if (email == null || deviceName == null) return false;
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        return redisTemplate != null && redisTemplate.hasKey(key);
    }

    @Override
    public void removeBlacklistedAccessTokenOfEmailAndDevice(String email, String deviceName) {
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.delete(key);
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) return userDetails.getUsername();
        if (principal instanceof DefaultOAuth2User oauthUser) {
            String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            return getStrategyForProvider(provider).getUsername(oauthUser.getAttributes());
        }
        throw new UnsupportedPrincipalType("Unsupported principal: " + principal.getClass().getName());
    }

    private String extractRole(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) return "ROLE_" + user.getRole().name();
        if (principal instanceof UserDetails userDetails)
            return userDetails.getAuthorities()
                    .stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");
        if (principal instanceof DefaultOAuth2User) return "ROLE_USER";
        throw new UnsupportedPrincipalType("Unsupported principal: " + principal.getClass().getName());
    }

    private OAuth2UserInfoStrategy getStrategyForProvider(String provider) {
        return strategies.stream()
                .filter(s -> s.getAuthProvider().name().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOAuth2Provider("Unsupported provider: " + provider));
    }
}
