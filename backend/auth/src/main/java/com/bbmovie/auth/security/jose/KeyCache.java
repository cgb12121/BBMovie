package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.jose.JoseKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.bbmovie.auth.security.jose.dto.KeyRotatedEvent;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    private volatile RSAKey activeRsaKey;
    private volatile SecretKey activeHmacKey;
    private volatile List<RSAKey> publicKeys;

    private final JwkKeyRepository keyRepo;

    @Autowired
    public KeyCache(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void initialize() {
        log.info("Initializing KeyCache. Attempting to load active key from database.");
        // Try to load an existing active key
        keyRepo.findAll().stream()
                .filter(JoseKey::isActive)
                .findFirst()
                .ifPresentOrElse(
                        this::setActiveKey,
                        () -> log.warn("No active key found in the database. KeyCache will start without an active key and wait for a KeyRotatedEvent.")
                );
        refreshPublicKeys();
        log.info("KeyCache initialized successfully.");
    }

    @EventListener
    public synchronized void handleKeyRotation(KeyRotatedEvent event) {
        log.info("Received JwkKeyRotatedEvent, refreshing keys: {}", event);
        if (event.getNewKey() != null) { // Caused by key rotation
            log.info("Key rotation event with new key (kid: {}). Updating active key.", event.getNewKey().getKid());
            setActiveKey(event.getNewKey());
            refreshPublicKeys();
        } else { // Caused by inactive key cleanup
            log.info("Inactive key cleanup event. Refreshing public keys.");
            refreshPublicKeys();
        }
    }

    private synchronized void setActiveKey(JoseKey activeKeyEntity) {
        log.info("Setting active key from entity with kid: {}", activeKeyEntity.getKid());
        switch (activeKeyEntity.getKeyType()) {
            case RSA -> updateRsaKey(activeKeyEntity);
            case HMAC -> updateHmacKey(activeKeyEntity);
            default -> throw new IllegalStateException("Invalid key type: " + activeKeyEntity.getKeyType());
        }
    }

    private void updateHmacKey(JoseKey activeKeyEntity) {
        if (activeKeyEntity.getHmacSecret() == null || activeKeyEntity.getHmacSecret().isBlank()) {
            throw new IllegalStateException("Active HMAC key entity is missing the secret for kid: " + activeKeyEntity.getKid());
        }
        byte[] decodedKey = Base64.getUrlDecoder().decode(activeKeyEntity.getHmacSecret());
        if (decodedKey.length == 0) {
            throw new IllegalStateException("Active HMAC key is empty after Base64 decoding for kid: " + activeKeyEntity.getKid());
        }
        this.activeHmacKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
        this.activeRsaKey = null; // Ensure RSA key is cleared
        log.info("Active key is now HMAC (kid: {}), loaded successfully.", activeKeyEntity.getKid());
    }

    private void updateRsaKey(JoseKey activeKeyEntity) {
        try {
            this.activeRsaKey = RSAKey.parse(activeKeyEntity.getPrivateJwk());
            this.activeHmacKey = null; // Ensure HMAC key is cleared
            log.info("Active key is now RSA (kid: {}), loaded successfully.", activeRsaKey.getKeyID());
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse active RSA key with kid: " + activeKeyEntity.getKid(), e);
        }
    }

    private synchronized void refreshPublicKeys() {
        log.debug("Refreshing public RSA keys from database.");
        this.publicKeys = keyRepo.findAll().stream()
                .filter(key -> key.getKeyType() == JoseKey.KeyType.RSA && key.getPublicJwk() != null)
                .sorted(Comparator.comparing(JoseKey::getCreatedDate).reversed()) // Newest first
                .map(key -> {
                    try {
                        return RSAKey.parse(key.getPublicJwk());
                    } catch (ParseException e) {
                        log.error("Invalid public RSA key in database with kid: {}. Skipping.", key.getKid(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        log.info("Found and refreshed {} public RSA keys.", this.publicKeys.size());
    }
}