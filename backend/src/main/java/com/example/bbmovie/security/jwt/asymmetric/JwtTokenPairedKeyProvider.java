package com.example.bbmovie.security.jwt.asymmetric;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.UnsupportedOAuth2Provider;
import com.example.bbmovie.exception.UnsupportedPrincipalType;
import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

    private final int jwtAccessTokenExpirationInMs;
    private final int jwtRefreshTokenExpirationInMs;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final RedisTemplate<Object, Object> redisTemplate;

    public JwtTokenPairedKeyProvider(
            @Value("${app.jwt-private-key}") String privateKeyStr,
            @Value("${app.jwt-public-key}") String publicKeyStr,
            @Value("${app.jwt-access-expiration-milliseconds}") int jwtAccessTokenExpirationInMs,
            @Value("${app.jwt-refresh-expiration-milliseconds}") int jwtRefreshTokenExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate
    ) throws Exception {
        this.jwtAccessTokenExpirationInMs = jwtAccessTokenExpirationInMs;
        this.jwtRefreshTokenExpirationInMs = jwtRefreshTokenExpirationInMs;
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
        return generateToken(authentication, jwtAccessTokenExpirationInMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jwtRefreshTokenExpirationInMs);
    }

    private String generateToken(Authentication authentication, int expirationInMs) {
        String username = getUsernameFromAuthentication(authentication);
        String role = getRoleFromAuthentication(authentication);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
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
                default -> throw new UnsupportedOAuth2Provider("Unsupported provider or missing email");
            };
        }
        throw new UnsupportedPrincipalType("Unsupported principal type: " + principal.getClass().getName());
    }

    private String getRoleFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        log.info("getRoleFromAuthentication: {}", principal);
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
        throw new UnsupportedPrincipalType("Unsupported principal type: " + principal.getClass().getName());
    }

    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String role = claims.get("role", String.class);
            return Collections.singletonList(role);
        } catch (ExpiredJwtException e) {
            String role = e.getClaims().get("role", String.class);
            return Collections.singletonList(role);
        }
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

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public void invalidateAccessToken(String accessToken) {
        try {
            String blacklistKey = "jwt-blacklist:" + accessToken;
            redisTemplate.opsForValue().set(
                    blacklistKey,
                    "invalidated",
                    getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
            );
        } catch (ExpiredJwtException e) {
            redisTemplate.opsForValue().set(
                    "jwt-blacklist:" + accessToken,
                    "invalidated",
                    15,
                    TimeUnit.MINUTES
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("jwt-blacklist:" + token);
    }

    public void invalidateAccessTokenByEmailAndDevice(String email, String deviceId) {
        String key = "jwt-block-access-token:" + email + ":" + deviceId;
        redisTemplate.opsForValue().set(key, "true", 15, TimeUnit.MINUTES);
    }

    public boolean isAccessTokenBlockedForEmailAndDevice(String email, String deviceId) {
        String key = "jwt-block-access-token:" + email + ":" + deviceId;
        return redisTemplate.hasKey(key);
    }

    public void removeJwtBlockAccessTokenOfEmailAndDevice(String email, String deviceId) {
        String key = "jwt-block-access-token:" + email + ":" + deviceId;
        redisTemplate.delete(key);
    }
}
