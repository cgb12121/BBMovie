package com.bbmovie.auth.security.jose.nimbus;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UnsupportedOAuth2Provider;
import com.bbmovie.auth.exception.UnsupportedPrincipalType;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.KeyCache;
import com.bbmovie.auth.security.jose.dto.TokenPair;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.log4j.Log4j2;
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

import static com.example.common.entity.JoseConstraint.*;
import static com.example.common.entity.JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JosePayload.*;
import static com.example.common.entity.JoseConstraint.JosePayload.ABAC.*;
import static com.example.common.entity.JoseConstraint.JwtType.JWE;

/**
 * JOSE provider using the `nimbus-jose-jwt` library for JWE with RSA algorithms (e.g., RSA-OAEP-256).
 * This implementation uses an asymmetric RSA key pair.
 * - The public key is used for encrypting tokens.
 * - The private key is used for decryption.
 * It dynamically retrieves the keys from the JwkKeyCache to support key rotation.
 */
@Log4j2
@Component("jweRsaNimbus")
public class JweRsaNimbusProvider implements JoseProviderStrategy {

    private final int jwtAccessTokenExpirationInMs;
    private final int jwtRefreshTokenExpirationInMs;
    private final KeyCache keyCache;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final List<OAuth2UserInfoStrategy> strategies;

    public JweRsaNimbusProvider(
            @Value("${app.jose.expiration.access-token}") int jwtAccessTokenExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jwtRefreshTokenExpirationInMs,
            KeyCache keyCache,
            RedisTemplate<Object, Object> redisTemplate,
            List<OAuth2UserInfoStrategy> strategies
    ) {
        this.jwtAccessTokenExpirationInMs = jwtAccessTokenExpirationInMs;
        this.jwtRefreshTokenExpirationInMs = jwtRefreshTokenExpirationInMs;
        this.keyCache = keyCache;
        this.redisTemplate = redisTemplate;
        this.strategies = strategies;
    }

    @Override
    public TokenPair generateTokenPair(Authentication authentication, User loggedInUser) {
        String refreshJti = UUID.randomUUID().toString();
        String sid = UUID.randomUUID().toString();
        String refreshToken = generateToken(authentication, jwtRefreshTokenExpirationInMs, sid, loggedInUser, refreshJti, null);
        String accessToken = generateToken(authentication, jwtAccessTokenExpirationInMs, sid, loggedInUser, UUID.randomUUID().toString(), refreshJti);
        return new TokenPair(accessToken, refreshToken);
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
            RSAKey currentActiveKey = keyCache.getActiveRsaKey();
            String username = extractUsername(authentication);
            String role = extractRole(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                    .contentType("JWT")
                    .keyID(currentActiveKey.getKeyID())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim(ROLE, role)
                    .claim(SUBSCRIPTION_TIER, loggedInUser.getSubscriptionTier().name())
                    .claim(AGE, loggedInUser.getAge())
                    .claim(REGION, loggedInUser.getRegion().name())
                    .claim(PARENTAL_CONTROLS_ENABLED, loggedInUser.isParentalControlsEnabled())
                    .claim(IS_ACCOUNTING_ENABLED, loggedInUser.getIsEnabled())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .jwtID(UUID.randomUUID().toString())
                    .claim(SID, sid)
                    .build();

            EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

            RSAKey publicKey = currentActiveKey.toPublicJWK();
            JWEEncrypter encrypter = new RSAEncrypter(publicKey);

            encryptedJWT.encrypt(encrypter);

            return encryptedJWT.serialize();
        } catch (Exception e) {
            log.error("Token encryption error: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate JWE token", e);
        }
    }

    private String generateToken(
            Authentication authentication, long expirationInMs, String sid, User loggedInUser,
            String jti, String issuer
    ) {
        try {
            RSAKey currentActiveKey = keyCache.getActiveRsaKey();
            String username = extractUsername(authentication);
            String role = extractRole(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                    .contentType("JWT")
                    .keyID(currentActiveKey.getKeyID())
                    .build();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(username)
                    .claim(ROLE, role)
                    .claim(SUBSCRIPTION_TIER, loggedInUser.getSubscriptionTier().name())
                    .claim(AGE, loggedInUser.getAge())
                    .claim(REGION, loggedInUser.getRegion().name())
                    .claim(PARENTAL_CONTROLS_ENABLED, loggedInUser.isParentalControlsEnabled())
                    .claim(IS_ACCOUNTING_ENABLED, loggedInUser.getIsEnabled())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .jwtID(jti)
                    .claim(SID, sid)
                    .build();

            EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);

            RSAKey publicKey = currentActiveKey.toPublicJWK();
            JWEEncrypter encrypter = new RSAEncrypter(publicKey);

            encryptedJWT.encrypt(encrypter);

            return encryptedJWT.serialize();
        } catch (Exception e) {
            log.error("[Experimental] Token generation failed: {}", e.getMessage());
            throw new IllegalStateException("JWT generation failed", e);
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
            // Use the current active key for decryption
            RSADecrypter decrypter = new RSADecrypter(keyCache.getActiveRsaKey().toRSAPrivateKey());
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
                        String role = (String) jwt.getJWTClaimsSet().getClaim(ROLE);
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
                        return (String) jwt.getJWTClaimsSet().getClaim(JTI);
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
                        return (String) jwt.getJWTClaimsSet().getClaim(SID);
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

    @Override
    public JwtType getType() {
        return JWE;
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
