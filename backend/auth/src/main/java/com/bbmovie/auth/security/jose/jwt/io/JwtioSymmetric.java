package com.bbmovie.auth.security.jose.jwt.io;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UnsupportedOAuth2Provider;
import com.bbmovie.auth.exception.UnsupportedPrincipalType;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.config.TokenPair;
import com.example.common.annotation.Experimental;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
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

import static com.example.common.entity.JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JosePayload.*;
import static com.example.common.entity.JoseConstraint.JosePayload.ABAC.*;

@Log4j2
@SuppressWarnings("squid:S6830")
@Component("jwt.io.hmac")
public class JwtioSymmetric implements JoseProviderStrategy {

    private final int jwtAccessExpirationInMs;
    private final int jwtRefreshExpirationInMs;
    private final SecretKey secretKey;
    private final RedisTemplate<Object, Object> redisTemplate;

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

    @Experimental
    @Override
    public TokenPair generateTokenPair(Authentication authentication, User loggedInUser) {
        String refreshJti = UUID.randomUUID().toString();
        String sid = UUID.randomUUID().toString();
        String refreshToken = generateToken(authentication, jwtRefreshExpirationInMs, sid, loggedInUser, refreshJti, null);
        String accessToken = generateToken(authentication, jwtAccessExpirationInMs, sid, loggedInUser, UUID.randomUUID().toString(), refreshJti);
        return new TokenPair(accessToken, refreshToken);
    }

    public String generateAccessToken(Authentication authentication, String sid, User loggedInUser) {
       return generateToken(authentication, jwtAccessExpirationInMs, sid, loggedInUser);
    }

    public String generateRefreshToken(Authentication authentication, String sid,  User loggedInUser) {
        return generateToken(authentication, jwtRefreshExpirationInMs, sid, loggedInUser);
    }

    public String generateToken(Authentication authentication, int expirationInMs, String sid, User loggedInUser) {
        String username = getUsernameFromAuthentication(authentication);
        String role = getRoleFromAuthentication(authentication);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JTI, UUID.randomUUID().toString());
        claims.put(SID, sid);
        claims.put(ROLE, role);
        claims.put(SUBSCRIPTION_TIER, loggedInUser.getSubscriptionTier().name());
        claims.put(AGE, loggedInUser.getAge());
        claims.put(REGION, loggedInUser.getRegion().name());
        claims.put(PARENTAL_CONTROLS_ENABLED, loggedInUser.isParentalControlsEnabled());
        claims.put(IS_ACCOUNTING_ENABLED, loggedInUser.getIsEnabled());

        return Jwts.builder()
                .setSubject(username)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Experimental
    private String generateToken(
            Authentication authentication, long expirationInMs, String sid, User loggedInUser,
            String jti, String issuer
    ) {
        String username = getUsernameFromAuthentication(authentication);
        String role = getRoleFromAuthentication(authentication);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JTI, jti);
        claims.put(SID, sid);
        claims.put(ROLE, role);
        claims.put(SUBSCRIPTION_TIER, loggedInUser.getSubscriptionTier().name());
        claims.put(AGE, loggedInUser.getAge());
        claims.put(REGION, loggedInUser.getRegion().name());
        claims.put(PARENTAL_CONTROLS_ENABLED, loggedInUser.isParentalControlsEnabled());
        claims.put(IS_ACCOUNTING_ENABLED, loggedInUser.getIsEnabled());

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer) // Set issuer (refresh token's jti), null for refresh token
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
        try {
            Claims claims = extractClaims(token);
            String role = claims.get(ROLE, String.class);
            return List.of(role);
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            String role = claims.get(ROLE, String.class);
            return List.of(role);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return claims.getSubject();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration();
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return claims.getExpiration();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public String getJtiFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.get(JTI, String.class);
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return claims.get(JTI, String.class);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public String getSidFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.get(SID, String.class);
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return claims.get(SID, String.class);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Map<String, Object> getClaimsFromToken(String token) {
        try {
            return extractClaims(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Map<String, Object> getOnlyABACClaimsFromToken(String token) {
        try {
            Map<String, Object> claims = getClaimsFromToken(token);
            return getOnlyABACFromClaims(claims);
        } catch (ExpiredJwtException e) {
            Map<String, Object> claims = e.getClaims();
            return getOnlyABACFromClaims(claims);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Map<String, Object> getOnlyABACFromClaims(Map<String, Object> claims) {
        claims.remove(JTI);
        claims.remove(SID);
        claims.remove(ROLE);
        claims.remove(SUB);
        claims.remove(EXP);
        claims.remove(IAT);
        claims.remove(ISS);
        return claims;
    }

    @Override
    public Date getIssuedAtFromToken(String token) {
        try {
            return extractClaims(token).getIssuedAt();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getIssuedAt();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
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

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public boolean isTokenInLogoutBlacklist(String sid) {
        return redisTemplate != null && redisTemplate.hasKey(JWT_LOGOUT_BLACKLIST_PREFIX + sid);
    }

    @Override
    public void addTokenToLogoutBlacklist(String sid) {
        String key = JWT_LOGOUT_BLACKLIST_PREFIX + sid;
        redisTemplate.opsForValue().set(key, "", 15, TimeUnit.MINUTES);
    }

    @Override
    public void removeFromLogoutBlacklist(String sid) {
        String key = JWT_LOGOUT_BLACKLIST_PREFIX + sid;
        redisTemplate.delete(key);
    }

    @Override
    public boolean isTokenInABACBlacklist(String sid) {
        String key = JWT_ABAC_BLACKLIST_PREFIX + sid;
        return redisTemplate != null && redisTemplate.hasKey(key);
    }

    @Override
    public void addTokenToABACBlacklist(String sid) {
        String key = JWT_ABAC_BLACKLIST_PREFIX + sid;
        redisTemplate.opsForValue().set(key, "", 15, TimeUnit.MINUTES);
    }

    @Override
    public void removeTokenFromABACBlacklist(String sid) {
        String key = JWT_ABAC_BLACKLIST_PREFIX + sid;
        redisTemplate.delete(key);
    }
}