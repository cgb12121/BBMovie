package com.bbmovie.auth.security.jose.config;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * A service class for generating and managing RSA keys and JSON Web Keys (JWKs).
 * This utility provides methods to manage the cryptographic keys necessary for
 * secure operations such as signing and verifying JSON Web Tokens (JWTs).
 */
@Service
public class RSAKeyService {

    private RSAKeyService() {
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