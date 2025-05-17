package com.example.bbmovie.security.jwt.asymmetric;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.UnsupportedOAuth2Provider;
import com.example.bbmovie.exception.UnsupportedPrincipalType;
import com.example.bbmovie.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
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
    private final List<OAuth2UserInfoStrategy> strategies;

    public JwtTokenPairedKeyProvider(
            @Value("${app.jwt-private-key}") String privateKeyStr,
            @Value("${app.jwt-public-key}") String publicKeyStr,
            @Value("${app.jwt-access-expiration-milliseconds}") int jwtAccessTokenExpirationInMs,
            @Value("${app.jwt-refresh-expiration-milliseconds}") int jwtRefreshTokenExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate,
            List<OAuth2UserInfoStrategy> strategies
    ) throws Exception {
        this.jwtAccessTokenExpirationInMs = jwtAccessTokenExpirationInMs;
        this.jwtRefreshTokenExpirationInMs = jwtRefreshTokenExpirationInMs;
        this.privateKey = getPrivateKeyFromString(privateKeyStr);
        this.publicKey = getPublicKeyFromString(publicKeyStr);
        this.redisTemplate = redisTemplate;
        this.strategies = strategies;
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

/*
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
*/

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
        try {
            return parseClaims(token).getExpiration();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        }
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

    public void invalidateAccessTokenByEmailAndDevice(String email, String deviceName) {
        String key = "jwt-blacklist:" + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.opsForValue().set(key, "true", 15, TimeUnit.MINUTES);
    }

    public boolean isAccessTokenBlacklistedForEmailAndDevice(String email, String deviceName) {
        String key = "jwt-blacklist:" + email + ":" + StringUtils.deleteWhitespace(deviceName);
        return redisTemplate.hasKey(key);
    }

    public void removeJwtBlockAccessTokenOfEmailAndDevice(String email, String deviceName) {
        String key = "jwt-blacklist:" + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.delete(key);
    }
}
