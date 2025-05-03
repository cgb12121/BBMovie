package com.example.bbmovie.security.jwt.asymmetric;

import com.example.bbmovie.entity.User;
import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Base64;

@Component
@Log4j2
public class JwtTokenPairedKeyProvider {

    private final int jwtExpirationInMs;
    private final int jwtRefreshExpirationInMs;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final RedisTemplate<Object, Object> redisTemplate;

    public JwtTokenPairedKeyProvider(
            @Value("${app.jwt-private-key}") String privateKeyStr,
            @Value("${app.jwt-public-key}") String publicKeyStr,
            @Value("${app.jwt-expiration-milliseconds}") int jwtExpirationInMs,
            @Value("${app.jwt-refresh-expiration-milliseconds}") int jwtRefreshExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate
    ) throws Exception {
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.jwtRefreshExpirationInMs = jwtRefreshExpirationInMs;
        this.privateKey = getPrivateKeyFromString(privateKeyStr);
        this.publicKey = getPublicKeyFromString(publicKeyStr);
        this.redisTemplate = redisTemplate;
    }

    private PrivateKey getPrivateKeyFromString(String key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtExpirationInMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jwtRefreshExpirationInMs);
    }

    private String generateToken(Authentication authentication, int expirationInMs) {
        String username = getUsernameFromAuthentication(authentication);
        String role = getRoleFromAuthentication(authentication);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

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
                default -> throw new IllegalStateException("Unsupported provider or missing email");
            };
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
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

    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        return Collections.singletonList(role);
    }

    public String getUsernameFromToken(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Date getExpirationDateFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    public void invalidateToken(String accessToken) {
        try {
            String blacklistKey = "rsa-blacklist:" + accessToken;
            redisTemplate.opsForValue().set(
                    blacklistKey,
                    "invalidated",
                    getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (ExpiredJwtException e) {
            redisTemplate.opsForValue().set(
                    "rsa-blacklist:" + accessToken,
                    "invalidated",
                    jwtRefreshExpirationInMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("rsa-blacklist:" + token);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
