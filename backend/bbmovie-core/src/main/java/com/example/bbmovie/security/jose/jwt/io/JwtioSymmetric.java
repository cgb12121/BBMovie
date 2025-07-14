package com.example.bbmovie.security.jose.jwt.io;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.UnsupportedOAuth2Provider;
import com.example.bbmovie.exception.UnsupportedPrincipalType;
import com.example.bbmovie.security.jose.JoseProviderStrategy;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component("hmac")
public class JwtioSymmetric implements JoseProviderStrategy {

    private final int jwtAccessExpirationInMs;
    private final int jwtRefreshExpirationInMs;
    private final SecretKey secretKey;
    private final RedisTemplate<Object, Object> redisTemplate;
    private static final String JWT_BLACKLIST_PREFIX = "jose-blacklist:";

    public JwtioSymmetric(
            @Value("${app.jose.key.secret}") String jwtSecret,
            @Value("${app.jose.expiration.access-token}") int jwtAccessExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jwtRefreshExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate
    ) {
        this.jwtAccessExpirationInMs = jwtAccessExpirationInMs;
        this.jwtRefreshExpirationInMs = jwtRefreshExpirationInMs;
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(Authentication authentication) {
       return generateToken(authentication, jwtAccessExpirationInMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jwtRefreshExpirationInMs);
    }

    public String generateToken(Authentication authentication, int expirationInMs) {
        String username = getUsernameFromAuthentication(authentication);
        String role = getRoleFromAuthentication(authentication);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("sid", UUID.randomUUID().toString());

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .addClaims(claims)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
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
        throw new UnsupportedPrincipalType(
                "Unsupported principal type: " + principal.getClass().getName()
        );
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
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        String role = claims.get("role", String.class);
        if (role == null) {
            throw new IllegalStateException("Role claim missing in token");
        }
        return List.of(role);
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return claims.getSubject();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
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