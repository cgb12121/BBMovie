package com.bbmovie.auth.security.jose.jwt.io;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.exception.UnsupportedOAuth2Provider;
import com.bbmovie.auth.exception.UnsupportedPrincipalType;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.config.JoseConstraint;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
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
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
@SuppressWarnings("squid:S6830")
@Component("jwt.io.rsa")
public class JwtioAsymmetric implements JoseProviderStrategy {

    private final int jwtAccessTokenExpirationInMs;
    private final int jwtRefreshTokenExpirationInMs;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final List<OAuth2UserInfoStrategy> strategies;

    public JwtioAsymmetric(
            @Value("${app.jose.key.private}") String privateKeyStr,
            @Value("${app.jose.key.public}") String publicKeyStr,
            @Value("${app.jose.expiration.access-token}") int jwtAccessTokenExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jwtRefreshTokenExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate,
            List<OAuth2UserInfoStrategy> strategies
    ) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.jwtAccessTokenExpirationInMs = jwtAccessTokenExpirationInMs;
        this.jwtRefreshTokenExpirationInMs = jwtRefreshTokenExpirationInMs;
        this.privateKey = getPrivateKeyFromString(privateKeyStr);
        this.publicKey = getPublicKeyFromString(publicKeyStr);
        this.redisTemplate = redisTemplate;
        this.strategies = strategies;
    }

    private PrivateKey getPrivateKeyFromString(String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] bytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private PublicKey getPublicKeyFromString(String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] bytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    @Override
    public String generateAccessToken(Authentication authentication, String sid, User loggedInUser) {
        return generateToken(authentication, jwtAccessTokenExpirationInMs, sid, loggedInUser);
    }

    @Override
    public String generateRefreshToken(Authentication authentication, String sid, User loggedInUser) {
        return generateToken(authentication, jwtRefreshTokenExpirationInMs, sid, loggedInUser);
    }

    private String generateToken(Authentication authentication, int expirationInMs, String sid, User loggedInUser) {
        String username = getUsernameFromAuthentication(authentication);
        String role = getRoleFromAuthentication(authentication);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JoseConstraint.JosePayload.JTI, UUID.randomUUID().toString());
        claims.put(JoseConstraint.JosePayload.SID, sid);
        claims.put(JoseConstraint.JosePayload.ROLE, role);
        claims.put(JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER, loggedInUser.getSubscriptionTier().name());
        claims.put(JoseConstraint.JosePayload.ABAC.AGE, loggedInUser.getAge());
        claims.put(JoseConstraint.JosePayload.ABAC.REGION, loggedInUser.getRegion().name());
        claims.put(JoseConstraint.JosePayload.ABAC.PARENTAL_CONTROLS_ENABLED, loggedInUser.isParentalControlsEnabled());

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .addClaims(claims)
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
        String rolePrefix = "ROLE_";
        if (principal instanceof User user) {
            return rolePrefix + user.getRole().name();
        } else if (principal instanceof UserDetails userDetails) {
            return userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse(rolePrefix + Role.USER.name());
        } else if (principal instanceof DefaultOAuth2User) {
            return rolePrefix + Role.USER.name();
        }
        throw new UnsupportedPrincipalType(
                "Unsupported principal type: " + principal.getClass().getName()
        );
    }

    @Override
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            String role = claims.get(JoseConstraint.JosePayload.ROLE, String.class);
            return Collections.singletonList(role);
        } catch (ExpiredJwtException e) {
            String role = e.getClaims().get(JoseConstraint.JosePayload.ROLE, String.class);
            return Collections.singletonList(role);
        } catch (JwtException e) {
            log.error("Invalid JWT token when get roles: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        } catch (JwtException e) {
            log.error("Invalid JWT token when get username: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public String getJtiFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.get(JoseConstraint.JosePayload.JTI, String.class);
        } catch (ExpiredJwtException e) {
            return (e.getClaims().get(JoseConstraint.JosePayload.JTI, String.class));
        } catch (JwtException e) {
            log.error("Invalid JWT token when get jti: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public String getSidFromToken(String token) {
        try {
            return extractClaims(token).get(JoseConstraint.JosePayload.SID, String.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get(JoseConstraint.JosePayload.SID, String.class);
        } catch (JwtException e) {
            log.error("Invalid JWT token when get sid: {}", e.getMessage());
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
            log.error("Invalid JWT token when get claims: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Map<String, Object> getOnlyABACClaimsFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return getOnlyABACFromClaims(claims);
        } catch (ExpiredJwtException e) {
            return getOnlyABACFromClaims(e.getClaims());
        } catch (JwtException e) {
            log.error("Invalid JWT token when get abac: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Map<String, Object> getOnlyABACFromClaims(Map<String, Object> claims) {
        claims.remove(JoseConstraint.JosePayload.JTI);
        claims.remove(JoseConstraint.JosePayload.SID);
        claims.remove(JoseConstraint.JosePayload.ROLE);
        claims.remove(JoseConstraint.JosePayload.SUB);
        claims.remove(JoseConstraint.JosePayload.EXP);
        claims.remove(JoseConstraint.JosePayload.IAT);
        claims.remove(JoseConstraint.JosePayload.ISS);
        return claims;
    }

    @Override
    public Date getIssuedAtFromToken(String token) {
        try {
            return extractClaims(token).getIssuedAt();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getIssuedAt();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public boolean isTokenInLogoutBlacklist(String sid) {
        return redisTemplate.hasKey(JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX + sid);
    }

    @Override
    public void addTokenToLogoutBlacklist(String sid) {
        String key = JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX + sid;
        redisTemplate.opsForValue().set(key, "", 15, TimeUnit.MINUTES);
    }

    @Override
    public void removeFromLogoutBlacklist(String sid) {
        String key = JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX + sid;
        redisTemplate.delete(key);
    }

    @Override
    public boolean isTokenInABACBlacklist(String sid) {
        String key = JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX + sid;
        return redisTemplate.hasKey(key);
    }

    @Override
    public void addTokenToABACBlacklist(String sid) {
        String key = JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX + sid;
        redisTemplate.opsForValue().set(key, "", 15, TimeUnit.MINUTES);
    }

    @Override
    public void removeTokenFromABACBlacklist(String sid) {
        String key = JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX + sid;
        redisTemplate.delete(key);
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        try {
            return extractClaims(token).getExpiration();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
