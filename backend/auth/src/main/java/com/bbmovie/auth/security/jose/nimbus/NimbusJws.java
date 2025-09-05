package com.bbmovie.auth.security.jose.nimbus;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UnsupportedOAuth2Provider;
import com.bbmovie.auth.exception.UnsupportedPrincipalType;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.config.TokenPair;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.example.common.annotation.Experimental;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import static com.example.common.entity.JoseConstraint.*;
import static com.example.common.entity.JoseConstraint.JosePayload.*;
import static com.example.common.entity.JoseConstraint.JosePayload.ABAC.*;
import static com.example.common.entity.JoseConstraint.JwtType.JWS;

@Log4j2
@Component("nimbusJws")
public class NimbusJws implements JoseProviderStrategy {

    private final int jwtAccessTokenExpirationInMs;
    private final int jwtRefreshTokenExpirationInMs;
    private final RSAKey activePrivateKey;
    private final List<RSAKey> publicKeys;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final List<OAuth2UserInfoStrategy> strategies;

    public NimbusJws(
            @Value("${app.jose.expiration.access-token}") int jwtAccessTokenExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jwtRefreshTokenExpirationInMs,
            @Qualifier("activePrivateKey") RSAKey activePrivateKey,
            List<RSAKey> publicKeys,
            RedisTemplate<Object, Object> redisTemplate,
            List<OAuth2UserInfoStrategy> strategies
    ) {
        this.jwtAccessTokenExpirationInMs = jwtAccessTokenExpirationInMs;
        this.jwtRefreshTokenExpirationInMs = jwtRefreshTokenExpirationInMs;
        this.activePrivateKey = activePrivateKey;
        this.publicKeys = publicKeys;
        this.redisTemplate = redisTemplate;
        this.strategies = strategies;
    }

    @Experimental
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
            String username = getUsernameFromAuthentication(authentication);
            String role = getRoleFromAuthentication(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

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

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .keyID(activePrivateKey.getKeyID())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new RSASSASigner(activePrivateKey.toRSAPrivateKey()));
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Token generation error: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate JWT", e);
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

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .keyID(activePrivateKey.getKeyID())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new RSASSASigner(activePrivateKey.toRSAPrivateKey()));
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("[Experimental] Token generation failed: {}", e.getMessage());
            throw new IllegalStateException("JWT generation failed", e);
        }
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

    @Override
    public boolean validateToken(String token) {
        Optional<SignedJWT> verifiedJwt = resolveAndVerify(token);
        if (verifiedJwt.isEmpty()) {
            return false;
        }
        try {
            return verifiedJwt.get().getJWTClaimsSet().getExpirationTime().after(new Date());
        } catch (ParseException e) {
            log.error("Failed to parse token expiration: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getSubject();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid JWK username", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Failed to parse username from JWK token"));
    }

    @Override
    public List<String> getRolesFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        String role = (String) jwt.getJWTClaimsSet().getClaim(ROLE);
                        return List.of(role);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid roles from JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to get roles from JWK token"));
    }

    @Override
    public Date getIssuedAtFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getIssueTime();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid iat from JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to get issued date from JWK token"));
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        return jwt.getJWTClaimsSet().getExpirationTime();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid exp from JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to get expiration date from JWK token"));
    }

    @Override
    public String getJtiFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        return (String) jwt.getJWTClaimsSet().getClaim(JTI);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid jwt from JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to get jti from JWK token"));
    }

    @Override
    public String getSidFromToken(String token) {
        return resolveAndVerify(token)
                .map(jwt -> {
                    try {
                        return (String) jwt.getJWTClaimsSet().getClaim(SID);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid sid from JWK", e);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Unable to get sid from JWK token"));
    }

    @Override
    public Map<String, Object> getClaimsFromToken(String token) {
        return resolveAndVerify(token).map(jwt ->{
            try {
                return jwt.getJWTClaimsSet().getClaims();
            } catch (Exception e) {
                log.error("Failed to parse claims: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid claims from JWK", e);
            }
        }).orElseThrow(() -> new IllegalArgumentException("Unable to get claims from JWK token"));
    }

    @Override
    public Map<String, Object> getOnlyABACClaimsFromToken(String token) {
        return resolveAndVerify(token).map(jwt ->{
            try {
                return getOnlyABACFromClaims(jwt.getJWTClaimsSet().getClaims());
            } catch (Exception e) {
                log.error("Failed to parse abac claims from JWK token: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid abac from JWK", e);
            }
        }).orElseThrow(() -> new IllegalArgumentException("Unable to get abac claims from JWK token"));
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

    private Optional<SignedJWT> resolveAndVerify(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Invalid token format (null, blank, or missing 3 JWT parts): '{}'", token);
            return Optional.empty();
        }

        try {
            SignedJWT jwt = SignedJWT.parse(token);
            String kid = jwt.getHeader().getKeyID();
            if (kid == null) {
                log.error("Token missing key ID (kid)");
                return Optional.empty();
            }

            for (RSAKey key : publicKeys) {
                if (key.getKeyID().equals(kid)) {
                    if (jwt.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                        log.info("Token verified with kid: {}", kid);
                        return Optional.of(jwt);
                    } else {
                        log.error("Token verification failed for kid: {}", kid);
                        return Optional.empty();
                    }
                }
            }
            log.error("No matching key found for kid: {}", kid);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to verify token: {}", e.getMessage());
            return Optional.empty();
        }
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
        return JWS;
    }
}