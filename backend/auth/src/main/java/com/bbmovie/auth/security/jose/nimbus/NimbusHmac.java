package com.bbmovie.auth.security.jose.nimbus;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UnsupportedOAuth2Provider;
import com.bbmovie.auth.exception.UnsupportedPrincipalType;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.config.TokenPair;
import com.example.common.annotation.Experimental;
import com.example.common.entity.JoseConstraint;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
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
@Component("nimbusHmac")
public class NimbusHmac implements JoseProviderStrategy {

    private final int jwtAccessExpirationInMs;
    private final int jwtRefreshExpirationInMs;
    private final String jwtSecret;
    private SecretKey secretKey;
    private final RedisTemplate<Object, Object> redisTemplate;

    public NimbusHmac(
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

    @Experimental
    @Override
    public TokenPair generateTokenPair(Authentication authentication, User loggedInUser) {
        String refreshJti = UUID.randomUUID().toString();
        String sid = UUID.randomUUID().toString();
        String refreshToken = generateToken(authentication, jwtRefreshExpirationInMs, sid, loggedInUser, refreshJti, null);
        String accessToken = generateToken(authentication, jwtAccessExpirationInMs, sid, loggedInUser, UUID.randomUUID().toString(), refreshJti);
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public String generateAccessToken(Authentication authentication, String sid, User loggedInUser) {
        return generateToken(authentication, jwtAccessExpirationInMs, sid, loggedInUser);
    }

    @Override
    public String generateRefreshToken(Authentication authentication, String sid, User loggedInUser) {
        return generateToken(authentication, jwtRefreshExpirationInMs, sid, loggedInUser);
    }

    public String generateToken(Authentication authentication, int expirationInMs, String sid, User loggedInUser) {
        try {
            String username = getUsernameFromAuthentication(authentication);
            String role = getRoleFromAuthentication(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim(JoseConstraint.JosePayload.ROLE, role)
                    .claim(JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER, loggedInUser.getSubscriptionTier().name())
                    .claim(JoseConstraint.JosePayload.ABAC.AGE, loggedInUser.getAge())
                    .claim(JoseConstraint.JosePayload.ABAC.REGION, loggedInUser.getRegion().name())
                    .claim(JoseConstraint.JosePayload.ABAC.PARENTAL_CONTROLS_ENABLED, loggedInUser.isParentalControlsEnabled())
                    .claim(JoseConstraint.JosePayload.ABAC.IS_ACCOUNTING_ENABLED, loggedInUser.getIsEnabled())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .jwtID(UUID.randomUUID().toString())
                    .claim(JoseConstraint.JosePayload.SID, sid)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new MACSigner(secretKey.getEncoded()));

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Token generation failed: {}", e.getMessage());
            throw new IllegalStateException("JWT generation failed", e);
        }
    }

    @Experimental
    private String generateToken(
            Authentication authentication, long expirationInMs, String sid, User loggedInUser,
            String jti, String issuer
    ) {
        try {
            String username = getUsernameFromAuthentication(authentication);
            String role = getRoleFromAuthentication(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(username)
                    .claim(JoseConstraint.JosePayload.ROLE, role)
                    .claim(JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER, loggedInUser.getSubscriptionTier().name())
                    .claim(JoseConstraint.JosePayload.ABAC.AGE, loggedInUser.getAge())
                    .claim(JoseConstraint.JosePayload.ABAC.REGION, loggedInUser.getRegion().name())
                    .claim(JoseConstraint.JosePayload.ABAC.PARENTAL_CONTROLS_ENABLED, loggedInUser.isParentalControlsEnabled())
                    .claim(JoseConstraint.JosePayload.ABAC.IS_ACCOUNTING_ENABLED, loggedInUser.getIsEnabled())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .jwtID(jti)
                    .claim(JoseConstraint.JosePayload.SID, sid)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new MACSigner(secretKey.getEncoded()));

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("[Experimental] Token generation failed: {}", e.getMessage());
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
            String role = (String) signedJWT.getJWTClaimsSet().getClaim(JoseConstraint.JosePayload.ROLE);
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
    public Date getIssuedAtFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getIssueTime();
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
    public String getJtiFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getClaim(JoseConstraint.JosePayload.JTI).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public String getSidFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getClaim(JoseConstraint.JosePayload.SID).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Map<String, Object> getClaimsFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    @Override
    public Map<String, Object> getOnlyABACClaimsFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return getOnlyABACFromClaims(signedJWT.getJWTClaimsSet().getClaims());
        } catch (Exception e) {
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
}