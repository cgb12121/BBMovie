package com.bbmovie.auth.security.jose.nimbus;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UnsupportedOAuth2Provider;
import com.bbmovie.auth.exception.UnsupportedPrincipalType;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.config.JoseConstraint;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.log4j.Log4j2;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component("nimbusJwe")
public class NimbusJwe implements JoseProviderStrategy {

    private final int jwtAccessTokenExpirationInMs;
    private final int jwtRefreshTokenExpirationInMs;
    private final RSAKey activePrivateKey;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final List<OAuth2UserInfoStrategy> strategies;

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
    public String generateAccessToken(Authentication authentication, String sid, User loggedInUser) {
        return generateToken(authentication, jwtAccessTokenExpirationInMs, sid, loggedInUser);
    }

    @Override
    public String generateRefreshToken(Authentication authentication, String sid, User loggedInUser) {
        return generateToken(authentication, jwtRefreshTokenExpirationInMs, sid, loggedInUser);
    }

    private String generateToken(Authentication authentication, int expirationInMs, String sid, User loggedInUser) {
        try {
            String username = extractUsername(authentication);
            String role = extractRole(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                    .contentType("JWT")
                    .keyID(activePrivateKey.getKeyID())
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
                        String role = (String) jwt.getJWTClaimsSet().getClaim(JoseConstraint.JosePayload.ROLE);
                        return List.of(role);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Invalid JWT role", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Failed to resolve username from token"));
    }

    @Override
    public Date getIssuedAtFromToken(String token) {
        return resolveAndDecrypt(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getIssueTime();
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Invalid JWT expiration", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Token invalid or unverified"));
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
    public String getJtiFromToken(String token) {
        return resolveAndDecrypt(token)
                .map(jwt -> {
                    try {
                        return (String) jwt.getJWTClaimsSet().getClaim(JoseConstraint.JosePayload.JTI);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid jti from JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to get jti from JWK"));
    }

    @Override
    public String getSidFromToken(String token) {
        return resolveAndDecrypt(token)
                .map(jwt -> {
                    try {
                        return (String) jwt.getJWTClaimsSet().getClaim(JoseConstraint.JosePayload.SID);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid sid from JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to get sid from JWK"));
    }

    @Override
    public Map<String, Object> getClaimsFromToken(String token) {
        return resolveAndDecrypt(token).map(jwt ->{
            try {
                return jwt.getJWTClaimsSet().getClaims();
            } catch (Exception e) {
                log.error("Failed to parse claims: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid claims from JWK", e);
            }
        }).orElseThrow(() -> new IllegalArgumentException("Unable to get claims from JWK"));
    }

    @Override
    public Map<String, Object> getOnlyABACClaimsFromToken(String token) {
        return resolveAndDecrypt(token).map(jwt ->{
            try {
                return getOnlyABACFromClaims(jwt.getJWTClaimsSet().getClaims());
            } catch (Exception e) {
                log.error("Failed to parse abac claims from JWK token: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid abac from JWK", e);
            }
        }).orElseThrow(() -> new IllegalArgumentException("Unable to get claims from JWK"));
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
