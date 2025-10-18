package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.jose.JoseKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.bbmovie.auth.security.jose.dto.KeyRotatedEvent;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import static com.bbmovie.auth.security.jose.AppKeyGenerator.generateNewKeyForRotation;
import static com.bbmovie.auth.security.jose.AppKeyGenerator.generateRsaKey;

/**
 * The {@code JwkRotation} class is responsible for managing JSON Web Key (JWK)
 * rotation and maintenance processes. It leverages scheduling to periodically
 * perform key rotation and cleanup of inactive keys, ensuring effective and
 * secure key lifecycle management.
 * <p>
 * This class integrates with the following components:
 * - {@code JwkKeyRepository}: Used for persistence and retrieval of key entities.
 * - {@code ApplicationEventPublisher}: Used to publish events such as
 *   {@link KeyRotatedEvent} after key rotation or cleanup.
 * - {@code JwkKeyCache}: Provides access to the active private key and cached
 *   public keys.
 * <p>
 * Core features include:
 * - Rotating JWK keys at scheduled intervals to ensure secure key management.
 * - Removing inactive keys periodically to clean up unused entries in the database.
 * - Providing application-scoped beans for the active private RSA key and publicly
 *   accessible RSA keys.
 * <p>
 * The key rotation mechanism generates a new RSA key and deactivates any currently
 * active keys, persisting changes to the repository. A {@link KeyRotatedEvent}
 * is published after successful rotation to notify other application components.
 * <p>
 * Thread safety is guaranteed for critical operations, such as key rotation and
 * cleanup, by leveraging synchronization to prevent concurrent execution.
 */
@Log4j2
@Configuration
@EnableScheduling
public class KeyRotation {

    private final JwkKeyRepository keyRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.jose.strategy}")
    private String configuredStrategy;

    @Autowired
    public KeyRotation(JwkKeyRepository keyRepo, ApplicationEventPublisher eventPublisher) {
        this.keyRepo = keyRepo;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void initializeKeys() {
        if (keyRepo.count() == 0) {
            log.info("No keys found in the database. Initializing a new key based on strategy '{}'", configuredStrategy);
            if (configuredStrategy.toLowerCase().contains("hmac")) {
                rotateToNewHmacKey();
            } else {
                rotateToNewRsaKey();
            }
        }
    }

    /**
     * Rotates the active JSON Web Key (JWK) at scheduled intervals.
     * <p>
     * This method is executed every 7 days as defined by the specified cron expression.
     * It deactivates all currently active JWKs, generates a new RSA key,
     * and saves the newly generated key as the active JWK. Additionally,
     * it publishes a {@link KeyRotatedEvent} to signal other components about the rotation.
     * <p>
     * Steps involved in the rotation process:
     * - Fetch all keys from the repository and filter for the active ones.
     * - Mark all active keys as inactive and persist the updates in the repository.
     * - Generate a new RSA key and create a corresponding {@link JoseKey} entity for it.
     * - Save the newly created key entity in the repository and log the rotation process details.
     * - Publish an event to notify other components of the key rotation.
     * <p>
     * Thread safety is ensured by using the synchronized keyword to prevent concurrent execution.
     *
     * @throws IllegalStateException if the generation of the new RSA key fails.
     *
     * This is an administrative action and is not scheduled.
     */
    //TODO: auto rotate
    public synchronized void rotateToNewRsaKey() {
        log.info("Begin to rotate to a new RSA JWK key.");
        deactivateAllKeys();

        try {
            RSAKey newRsaKey = generateRsaKey();
            JoseKey newKeyEntity = generateNewKeyForRotation(newRsaKey);
            keyRepo.save(newKeyEntity);
            log.info("Rotated to new RSA JWK key successfully. {}", newKeyEntity.toString());

            eventPublisher.publishEvent(new KeyRotatedEvent(newKeyEntity));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate new RSA key", e);
        }
    }

    /**
     * Rotates the active JSON Web Key (JWK) to a new HMAC key.
     * This is an administrative action and is not scheduled.
     */
    //TODO: auto rotate
    public synchronized void rotateToNewHmacKey() {
        log.info("Begin to rotate to a new HMAC JWK key.");
        deactivateAllKeys();

        JoseKey newKeyEntity = AppKeyGenerator.generateNewHmacKeyForRotation();
        keyRepo.save(newKeyEntity);
        log.info("Rotated to new HMAC JWK key successfully. {}", newKeyEntity.toString());

        eventPublisher.publishEvent(new KeyRotatedEvent(newKeyEntity));
    }

    private void deactivateAllKeys() {
        keyRepo.findAll().stream().filter(JoseKey::isActive).forEach(key -> {
            key.setActive(false);
            keyRepo.save(key);
        });
    }

    /**
     * Removes inactive JWK (JSON Web Key) entries from the database at scheduled intervals.
     * <p>
     * This method is executed every six hours based on the defined cron expression, ensuring
     * that the database is periodically cleaned up to remove unused or inactive keys. The process
     * involves the following steps:
     * <ul>
     *   - Fetches all the keys currently stored in the repository.
     *   - Delegates the removal decision and logic to the {@code removedInactiveKeys} method.
     *   - Publishes a {@link KeyRotatedEvent} to signal other components about potential key changes.
     * </ul>
     * The method employs synchronization to ensure thread safety during execution.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public synchronized void removeInactiveKeys() {
        log.info("Begin to remove inactive JWK keys.");
        List<JoseKey> allKeys = keyRepo.findAll();
        removedInactiveKeys(allKeys);
        eventPublisher.publishEvent(new KeyRotatedEvent());
    }

    /**
     * Removes inactive JWK keys from the given list of keys based on certain conditions.
     * If the total number of keys in the list exceeds five, this method removes inactive keys
     * that are relatively recent (not older than a specific cutoff time) and may delete the oldest
     * ones first to ensure only a maximum of five keys remain.
     *
     * @param keys A list of {@link JoseKey} objects to evaluate and potentially remove inactive keys from.
     */
    private void removedInactiveKeys(List<JoseKey> keys) {
        Duration tokenLifetime = Duration.ofMinutes(15);
        Instant cutoff = Instant.now().minus(tokenLifetime);

        if (keys.size() > 5) {
            keys.stream()
                    .filter(key -> {
                        Instant keyCreationInstant = key.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant();
                        return !key.isActive() && !keyCreationInstant.isBefore(cutoff);
                    })
                    .sorted(Comparator.comparing(JoseKey::getCreatedDate))
                    .limit(keys.size() - 5L)
                    .forEach(keyRepo::delete);
        }
    }
}
