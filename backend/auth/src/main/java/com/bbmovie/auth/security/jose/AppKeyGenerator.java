package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import java.util.Base64;

/**
 * A service class for generating and managing RSA keys and JSON Web Keys (JWKs).
 * This utility provides methods to manage the cryptographic keys necessary for
 * secure operations such as signing and verifying JSON Web Tokens (JWTs).
 */
public class AppKeyGenerator {

    private AppKeyGenerator() {
    }

    /**
     * Generates a new 256-bit secret key suitable for HMAC-SHA256 operations.
     *
     * @return a {@link SecretKey} instance.
     * @throws IllegalStateException if the HmacSHA256 algorithm is not available.
     */
    public static SecretKey generateHmacKey() {
        try {
            javax.crypto.KeyGenerator keyGen = javax.crypto.KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 algorithm not available", e);
        }
    }

    /**
     * Generates a new {@link JwkKey} entity for an HMAC secret key.
     *
     * @return an instance of {@link JwkKey} configured for HMAC.
     */
    public static JwkKey generateNewHmacKeyForRotation() {
        SecretKey secretKey = generateHmacKey();

        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        return JwkKey.builder()
                .kid(UUID.randomUUID().toString())
                .keyType(JwkKey.KeyType.HMAC)
                .hmacSecret(encodedKey)
                .isActive(true)
                .build();
    }

    /**
     * Generates a new RSA key pair with a 2048-bit key size and additional metadata.
     * The generated key includes a randomly generated key ID, the issue time, and
     * specifies the RS256 signing algorithm.
     *
     * @return an instance of {@link RSAKey} representing the generated RSA key pair along with its metadata.
     * @throws JOSEException if an error occurs during the RSA key generation process.
     */
    public static RSAKey generateRsaKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .algorithm(JWSAlgorithm.RS256)
                .generate();
    }

    /**
     * Generates a new {@link JwkKey} for key rotation using the provided RSA key.
     * The generated JWK key includes the key identifier (kid), private JWK in
     * serialized form, public JWK in serialized form, and is marked as active.
     *
     * @param rsaKey the RSA key used to generate the new JWK key. Must not be null.
     * @return an instance of {@link JwkKey} containing the new key details for rotation.
     */
    public static JwkKey generateNewKeyForRotation(RSAKey rsaKey) {
        return JwkKey.builder()
                .kid(rsaKey.getKeyID())
                .privateJwk(rsaKey.toJSONString())
                .publicJwk(rsaKey.toPublicJWK().toJSONString())
                .isActive(true)
                .build();
    }
}