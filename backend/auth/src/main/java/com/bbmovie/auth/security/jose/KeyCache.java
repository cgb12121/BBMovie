package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.bbmovie.auth.security.jose.dto.KeyRotatedEvent;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Comparator;
import java.util.List;

import static com.bbmovie.auth.security.jose.AppKeyGenerator.generateNewKeyForRotation;
import static com.bbmovie.auth.security.jose.AppKeyGenerator.generateRsaKey;

/**
 * A class responsible for caching and managing JSON Web Keys (JWK), supporting both RSA and HMAC keys,
 * used in cryptographic operations, such as token signing and verification.
 * <p>
 * This class interacts with a {@code JwkKeyRepository} to retrieve, store, and manage cryptographic keys.
 * It ensures the availability of the current active key (RSA or HMAC) and a list of all available public RSA keys
 * while handling key rotation events and refreshing the key cache as needed.
 * <p>
 * Thread safety is maintained through synchronization in critical operations.
 */
@Getter
@Slf4j
@Component
@SuppressWarnings("squid:S3077")
public class KeyCache {

    private volatile RSAKey activeRsaPrivateKey;
    private volatile SecretKey activeHmacSecretKey;
    private volatile List<RSAKey> publicKeys;

    private final JwkKeyRepository keyRepo;

    @Autowired
    public KeyCache(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
        refreshActiveKeys();
        refreshPublicKeys();
    }

    @EventListener
    public synchronized void handleKeyRotation(KeyRotatedEvent event) {
        log.info("Received JwkKeyRotatedEvent, refreshing keys: {}", event);
        refreshActiveKeys();
        this.publicKeys = refreshPublicKeys();
    }

    private synchronized void refreshActiveKeys() {
        List<JwkKey> activeKeys = keyRepo.findAll().stream().filter(JwkKey::isActive).toList();
        if (activeKeys.isEmpty()) {
            log.info("No active key found in DB, generating a new default RSA key.");
            try {
                RSAKey newRsaKey = generateRsaKey();
                JwkKey newKeyEntity = generateNewKeyForRotation(newRsaKey);
                keyRepo.save(newKeyEntity);
                activeKeys = List.of(newKeyEntity);
            } catch (JOSEException e) {
                throw new IllegalStateException("Failed to generate new default RSA key", e);
            }
        }

        JwkKey activeKeyEntity = activeKeys.getFirst(); // Assuming only one key is active at a time
        if (activeKeyEntity.getKeyType() == JwkKey.KeyType.RSA) {
            try {
                activeRsaPrivateKey = RSAKey.parse(activeKeyEntity.getPrivateJwk());
                activeHmacSecretKey = null; // Ensure HMAC key is cleared
                log.info("Active key is RSA (kid: {}), loaded successfully.", activeRsaPrivateKey.getKeyID());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse active RSA key: " + e.getMessage(), e);
            }
        } else if (activeKeyEntity.getKeyType() == JwkKey.KeyType.HMAC) {
            if (activeKeyEntity.getHmacSecret() == null || activeKeyEntity.getHmacSecret().isBlank()) {
                throw new IllegalStateException("Active HMAC key entity is missing the secret.");
            }
            activeHmacSecretKey = new SecretKeySpec(activeKeyEntity.getHmacSecret().getBytes(), "HmacSHA256");
            activeRsaPrivateKey = null; // Ensure the RSA key is cleared
            log.info("Active key is HMAC (kid: {}), loaded successfully.", activeKeyEntity.getKid());
        }
    }

    private synchronized List<RSAKey> refreshPublicKeys() {
        publicKeys = keyRepo.findAll().stream()
                .filter(key -> key.getKeyType() == JwkKey.KeyType.RSA && key.getPublicJwk() != null)
                .sorted(Comparator.comparing(JwkKey::getCreatedDate))
                .map(key -> {
                    try {
                        return RSAKey.parse(key.getPublicJwk());
                    } catch (Exception e) {
                        throw new IllegalStateException("Invalid public RSA key in database: " + key.getKid(), e);
                    }
                })
                .toList();
        return publicKeys;
    }
}