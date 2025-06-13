package com.example.bbmovie.security.jose.jwt.nimbus;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.UnsupportedOAuth2Provider;
import com.example.bbmovie.exception.UnsupportedPrincipalType;
import com.example.bbmovie.security.jose.JoseProviderStrategy;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component("hmacNimbus")
public class JoseHmacNimbusProvider implements JoseProviderStrategy {

    private final int jwtAccessExpirationInMs;
    private final int jwtRefreshExpirationInMs;
    private final String jwtSecret;
    private SecretKey secretKey;
    private final RedisTemplate<Object, Object> redisTemplate;
    private static final String JWT_BLACKLIST_PREFIX = "jwt-blacklist:";

    public JoseHmacNimbusProvider(
            @Value("${app.jose.key.secret}") String jwtSecret,
            @Value("${app.jose.expiration.access-token}") int jwtAccessExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jwtRefreshExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtAccessExpirationInMs = jwtAccessExpirationInMs;
        this.jwtRefreshExpirationInMs = jwtRefreshExpirationInMs;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void initKey() {
        this.secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
    }

    @Override
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtAccessExpirationInMs);
    }

    @Override
    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jwtRefreshExpirationInMs);
    }

    public String generateToken(Authentication authentication, int expirationInMs) {
        try {
            String username = getUsernameFromAuthentication(authentication);
            String role = getRoleFromAuthentication(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim("role", role)
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new MACSigner(secretKey.getEncoded()));

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Token generation failed: {}", e.getMessage());
            throw new IllegalStateException("JWT generation failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String getUsernameFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof DefaultOAuth2User oauth2User) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            String provider = token.getAuthorizedClientRegistrationId();
            Map<String, Object> attributes = oauth2User.getAttributes();

            return switch (provider) {
                case "github" -> attributes.get("login").toString();
                case "google", "facebook", "discord" -> attributes.get("email").toString();
                case "x" -> {
                    Map<String, Object> data = (Map<String, Object>) attributes.get("data");
                    if (data == null || data.get("username") == null) {
                        throw new UnsupportedOAuth2Provider("Missing 'username' in X provider data");
                    }
                    yield data.get("username").toString();
                }
                default -> throw new UnsupportedOAuth2Provider("Unsupported provider or missing email");
            };
        }
        throw new UnsupportedPrincipalType("Unsupported principal type: " + principal.getClass().getName());
    }

    private String getRoleFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return "ROLE_" + user.getRole().name();
        } else if (principal instanceof DefaultOAuth2User) {
            return "ROLE_USER";
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
    }

    @Override
    public List<String> getRolesFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(new MACVerifier(secretKey.getEncoded()))) {
                throw new IllegalArgumentException("JWT verification failed");
            }
            String role = (String) signedJWT.getJWTClaimsSet().getClaim("role");
            return List.of(role);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getExpirationTime();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.verify(new MACVerifier(secretKey.getEncoded()));
        } catch (Exception ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public void invalidateAccessTokenByEmailAndDevice(String email, String deviceName) {
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.opsForValue().set(key, "true", 15, TimeUnit.MINUTES);
    }

    @Override
    public boolean isAccessTokenBlacklistedForEmailAndDevice(String email, String deviceName) {
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        return redisTemplate != null && redisTemplate.hasKey(key);
    }

    @Override
    public void removeBlacklistedAccessTokenOfEmailAndDevice(String email, String deviceName) {
        String key = JWT_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.delete(key);
    }
}