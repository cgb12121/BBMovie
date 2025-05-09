//package com.example.bbmovie.security.jwt.symmetric;
//
//import com.example.bbmovie.entity.User;
//import io.jsonwebtoken.*;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.log4j.Log4j2;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.SecretKey;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
//@Log4j2
//@Component
//public class JwtTokenProvider {
//
//    private final int jwtExpirationInMs;
//    private final int jwtRefreshExpirationInMs;
//    private final SecretKey key;
//    private final RedisTemplate<Object, Object> redisTemplate;
//
//    public JwtTokenProvider(
//            @Value("${app.jwt-secret}") String jwtSecret,
//            @Value("${app.jwt-expiration-milliseconds}") int jwtExpirationInMs,
//            @Value("${app.jwt-refresh-expiration-milliseconds}") int jwtRefreshExpirationInMs,
//            RedisTemplate<Object, Object> redisTemplate
//    ) {
//        this.jwtExpirationInMs = jwtExpirationInMs;
//        this.jwtRefreshExpirationInMs = jwtRefreshExpirationInMs;
//        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
//        this.redisTemplate = redisTemplate;
//    }
//
//    public String generateAccessToken(Authentication authentication) {
//        String username = getUsernameFromAuthentication(authentication);
//        String role = getRoleFromAuthentication(authentication);
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
//
//        return Jwts.builder()
//                .setSubject(username)
//                .claim("role", role)
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(key)
//                .compact();
//    }
//
//    public String generateRefreshToken(Authentication authentication) {
//        String username = getUsernameFromAuthentication(authentication);
//        String role = getRoleFromAuthentication(authentication);
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationInMs);
//
//        return Jwts.builder()
//                .setSubject(username)
//                .claim("role", role)
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(key)
//                .compact();
//    }
//
//    private String getUsernameFromAuthentication(Authentication authentication) {
//        Object principal = authentication.getPrincipal();
//        if (principal instanceof UserDetails userDetails) {
//            return userDetails.getUsername();
//        } else if (principal instanceof DefaultOAuth2User oauth2User) {
//            OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
//            String provider = oAuth2Token.getAuthorizedClientRegistrationId();
//            Map<String, Object> attributes = oauth2User.getAttributes();
//
//            String email = switch (provider) {
//                case "github" -> attributes.get("login").toString();
//                case "google", "facebook", "discord" -> attributes.get("email").toString();
//                default -> "";
//            };
//
//            if (email.isEmpty()) {
//                throw new IllegalStateException("Email not found in OAuth2 user attributes");
//            }
//            return email;
//        }
//        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
//    }
//
//    private String getRoleFromAuthentication(Authentication authentication) {
//        Object principal = authentication.getPrincipal();
//        if (principal instanceof User user) {
//            return "ROLE_" + user.getRole().name();
//        } else if (principal instanceof DefaultOAuth2User) {
//            return "ROLE_USER";
//        }
//        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
//    }
//
//    public List<String> getRolesFromToken(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//        String role = claims.get("role", String.class);
//        if (role == null) {
//            throw new IllegalStateException("Role claim missing in token");
//        }
//        return List.of(role);
//    }
//
//    public String getUsernameFromToken(String token) {
//        try {
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(key)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//            log.info("Token subject (username): {}", claims.getSubject());
//            return claims.getSubject();
//        } catch (ExpiredJwtException e) {
//            // Extract claims from an expired token
//            Claims claims = e.getClaims();
//            log.info("Extracted username from expired token: {}", claims.getSubject());
//            return claims.getSubject();
//        } catch (JwtException e) {
//            log.error("Invalid token: {}", e.getMessage());
//            throw new IllegalArgumentException("Invalid JWT token", e);
//        }
//    }
//
//    public Date getExpirationDateFromToken(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//        log.info("Claims extracted: {}", claims);
//        return claims.getExpiration();
//    }
//
//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder()
//                    .setSigningKey(key)
//                    .build()
//                    .parseClaimsJws(token);
//            return true;
//        } catch (Exception ex) {
//            log.error("Token validation error: {}", ex.getMessage());
//            return false;
//        }
//    }
//
//    public void invalidateAccessToken(String accessToken) {
//        try {
//            String blacklistKey = "blacklist:" + accessToken;
//            redisTemplate.opsForValue().set(
//                    blacklistKey,
//                    "invalidated",
//                    getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis(),
//                    TimeUnit.MILLISECONDS
//            );
//        } catch (ExpiredJwtException ex) {
//            String blacklistKey = "blacklist:" + accessToken;
//            redisTemplate.opsForValue().set(
//                    blacklistKey,
//                    "invalidated",
//                    jwtRefreshExpirationInMs,
//                    TimeUnit.MILLISECONDS
//            );
//        }
//    }
//
//    public boolean isTokenBlacklisted(String token) {
//        return redisTemplate.hasKey("blacklist:" + token);
//    }
//}