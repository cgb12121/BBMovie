package com.bbmovie.auth.security.jose.config;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import static com.bbmovie.auth.security.jose.config.RSAKeyService.generateNewKeyForRotation;
import static com.bbmovie.auth.security.jose.config.RSAKeyService.generateRsaKey;

/**
 * A class responsible for caching and managing JSON Web Keys (JWK), specifically RSA keys,
 * used in cryptographic operations, such as token signing and verification.
 * <p>
 * This class interacts with a {@code JwkKeyRepository} to retrieve, store, and manage cryptographic keys.
 * It ensures the availability of the current active RSA private key and a list of all available public RSA keys
 * while handling key rotation events and refreshing the key cache as needed.
 * <p>
 * Thread safety is maintained through synchronization in critical operations.
 */
@Getter
@Slf4j
@Component
@SuppressWarnings("squid:S3077")
public class JwkKeyCache {

    private volatile RSAKey activePrivateKey;
    private volatile List<RSAKey> publicKeys;

    private final JwkKeyRepository keyRepo;

    @Autowired
    public JwkKeyCache(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
        refreshActivePrivateKey();
        refreshPublicKeys();
    }

    /**
     * Handles the key rotation event triggered by the {@link JwkKeyRotatedEvent}.
     * <p>
     * This method listens to the key rotation event and performs the following actions:
     * - Logs the occurrence of the JWK key rotation event.
     * - Refreshes the currently active private RSA key.
     * - Refreshes and updates the cached list of public RSA keys.
     * <p>
     * Thread safety is ensured through synchronization on the method.
     *
     * @param event the {@link JwkKeyRotatedEvent} that signals the occurrence of JWK key rotation.
     */
    @EventListener
    public synchronized void handleKeyRotation(JwkKeyRotatedEvent event) {
        log.info("Received JwkKeyRotatedEvent, refreshing keys: {}", event);
        refreshActivePrivateKey();
        this.publicKeys = refreshPublicKeys();
    }

    /**
     * Refreshes the currently active private RSA key by querying the repository and handling key rotation if necessary.
     * <p>
     * This method:
     * - Fetches all active keys from the repository.
     * - If no active keys are found, generates a new RSA key, creates a new key entity for rotation, saves it,
     *   and sets it as the active key.
     * - Ensures that the active private key is parsed and set correctly from the retrieved or newly created key.
     * <p>
     * Thread safety is ensured through method synchronization.
     *
     * @throws IllegalStateException if:
     *         - A new RSA key could not be generated due to a failure (e.g., JOSEException).
     *         - The active key could not be parsed successfully.
     */
    private synchronized void refreshActivePrivateKey() {
        List<JwkKey> activeKeys = keyRepo.findAll().stream().filter(JwkKey::isActive).toList();
        if (activeKeys.isEmpty()) {
            try {
                RSAKey newRsaKey = generateRsaKey();
                JwkKey newKeyEntity = generateNewKeyForRotation(newRsaKey);
                keyRepo.save(newKeyEntity);
                activeKeys = List.of(newKeyEntity);
                log.info("No key found, create a new key. {}", newKeyEntity);

            } catch (JOSEException e) {
                throw new IllegalStateException("Failed to generate new RSA key", e);
            }
            log.info("No active JwkKeys found");
        }
        try {
            activePrivateKey = RSAKey.parse(activeKeys.getFirst().getPrivateJwk());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid active key: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes and retrieves the list of public RSA keys available in the repository.
     * <p>
     * This method queries the repository for all keys, sorts them by their creation date,
     * and converts them into RSAKey objects. If any key is invalid during parsing,
     * an IllegalStateException is thrown.
     * <p>
     * Thread safety is ensured by synchronizing this method.
     * @throws IllegalStateException If any key is invalid during parsing.
     * @return the refreshed and sorted list of public RSA keys
     */
    private synchronized List<RSAKey> refreshPublicKeys() {
        publicKeys = keyRepo.findAll().stream()
                .sorted(Comparator.comparing(JwkKey::getCreatedDate))
                .map(key -> {
                    try {
                        return RSAKey.parse(key.getPrivateJwk());
                    } catch (Exception e) {
                        throw new IllegalStateException("Invalid key: " + key.getKid(), e);
                    }
                })
                .toList();
        return publicKeys;
    }
}