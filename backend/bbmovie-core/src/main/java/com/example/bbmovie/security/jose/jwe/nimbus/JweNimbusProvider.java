package com.example.bbmovie.security.jose.jwe.nimbus;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.JweException;
import com.example.bbmovie.exception.UnsupportedOAuth2Provider;
import com.example.bbmovie.exception.UnsupportedPrincipalType;
import com.example.bbmovie.security.jose.JoseProviderStrategy;
import com.example.bbmovie.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.EncryptedJWT;
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
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Base64;

@Log4j2
@Component("jweRsaNimbus")
public class JweNimbusProvider implements JoseProviderStrategy {

    private final int jweAccessTokenExpirationInMs;
    private final int jweRefreshTokenExpirationInMs;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final List<OAuth2UserInfoStrategy> strategies;
    private final RSADecrypter rsaDecrypter;
    private final RSAEncrypter rsaEncrypter;

    private static final String ALGORITHM = "RSA";
    private static final String JWE_BLACKLIST_PREFIX = "jose-blacklist:";

    public JweNimbusProvider(
            @Value("${app.jose.key.private}") String privateKeyStr,
            @Value("${app.jose.key.public}") String publicKeyStr,
            @Value("${app.jose.expiration.access-token}") int jweAccessTokenExpirationInMs,
            @Value("${app.jose.expiration.refresh-token}") int jweRefreshTokenExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate,
            List<OAuth2UserInfoStrategy> strategies
    ) {
        this.jweAccessTokenExpirationInMs = jweAccessTokenExpirationInMs;
        this.jweRefreshTokenExpirationInMs = jweRefreshTokenExpirationInMs;
        RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKeyFromString(privateKeyStr);
        RSAPublicKey publicKey = (RSAPublicKey) getPublicKeyFromString(publicKeyStr);
        this.rsaDecrypter = new RSADecrypter(privateKey);
        this.rsaEncrypter = new RSAEncrypter(publicKey);
        this.redisTemplate = redisTemplate;
        this.strategies = strategies;
    }

    private PrivateKey getPrivateKeyFromString(String key) {
        try {
            byte[] bytes = Base64.getDecoder().decode(key);
            return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            log.error("Failed to generate private key", e);
            throw new JweException("Failed to generate private key.");
        }
    }

    private PublicKey getPublicKeyFromString(String key) {
        try {
            byte[] bytes = Base64.getDecoder().decode(key);
            return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            log.error("Failed to generate public key", e);
            throw new JweException("Failed to generate public key.");
        }
    }

    @Override
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jweAccessTokenExpirationInMs);
    }

    @Override
    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jweRefreshTokenExpirationInMs);
    }

    private String generateToken(Authentication authentication, int expirationInMs) {
        try {
            String username = getUsernameFromAuthentication(authentication);
            String role = getRoleFromAuthentication(authentication);
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationInMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer("bbmovie-core")
                    .subject(username)
                    .claim("role", role)
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .jwtID(UUID.randomUUID().toString())
                    .claim("sid", UUID.randomUUID().toString())
                    .build();

            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM).build();
            EncryptedJWT encryptedJWT = new EncryptedJWT(header, claimsSet);
            encryptedJWT.encrypt(rsaEncrypter);

            return encryptedJWT.serialize();
        } catch (Exception e) {
            log.error("Token generation error: {}", e.getMessage());
            throw new JweException("Failed to generate Jwe token.");
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
    public List<String> getRolesFromToken(String token) {
        JWTClaimsSet claims = parseClaims(token);
        String role = (String) claims.getClaim("role");
        return List.of(role);
    }

    @Override
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        return parseClaims(token).getExpirationTime();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
            encryptedJWT.decrypt(rsaDecrypter);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private JWTClaimsSet parseClaims(String token) {
        try {
            EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
            encryptedJWT.decrypt(this.rsaDecrypter);
            return encryptedJWT.getJWTClaimsSet();
        } catch (ParseException | JOSEException e) {
            log.error("Failed to parse JWT token: ", e);
            throw new JweException("Unable to parse claims.");
        }
    }

    @Override
    public void invalidateAccessTokenByEmailAndDevice(String email, String deviceName) {
        String key = JWE_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.opsForValue().set(key, "true", 15, TimeUnit.MINUTES);
    }

    @Override
    public boolean isAccessTokenBlacklistedForEmailAndDevice(String email, String deviceName) {
        String key = JWE_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        return redisTemplate != null && redisTemplate.hasKey(key);
    }

    @Override
    public void removeBlacklistedAccessTokenOfEmailAndDevice(String email, String deviceName) {
        String key = JWE_BLACKLIST_PREFIX + email + ":" + StringUtils.deleteWhitespace(deviceName);
        redisTemplate.delete(key);
    }
}