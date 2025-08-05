package com.bbmovie.auth.security.jose.config;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import static com.bbmovie.auth.security.jose.config.RSAKeyService.generateNewKeyForRotation;
import static com.bbmovie.auth.security.jose.config.RSAKeyService.generateRsaKey;

/**
 * The {@code JwkRotation} class is responsible for managing JSON Web Key (JWK)
 * rotation and maintenance processes. It leverages scheduling to periodically
 * perform key rotation and cleanup of inactive keys, ensuring effective and
 * secure key lifecycle management.
 * <p>
 * This class integrates with the following components:
 * - {@code JwkKeyRepository}: Used for persistence and retrieval of key entities.
 * - {@code ApplicationEventPublisher}: Used to publish events such as
 *   {@link JwkKeyRotatedEvent} after key rotation or cleanup.
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
 * active keys, persisting changes to the repository. A {@link JwkKeyRotatedEvent}
 * is published after successful rotation to notify other application components.
 * <p>
 * Thread safety is guaranteed for critical operations, such as key rotation and
 * cleanup, by leveraging synchronization to prevent concurrent execution.
 */
@Log4j2
@Configuration
@EnableScheduling
public class JwkRotation {

    private final JwkKeyRepository keyRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final JwkKeyCache keyCache;

    @Autowired
    public JwkRotation(JwkKeyRepository keyRepo, ApplicationEventPublisher eventPublisher, JwkKeyCache keyCache) {
        this.keyRepo = keyRepo;
        this.eventPublisher = eventPublisher;
        this.keyCache = keyCache;
    }

    /**
     * Retrieves the currently active private RSA key from the key cache.
     *
     * @return the active private RSA key as an instance of {@link RSAKey}.
     */
    @Bean
    public RSAKey activePrivateKey() {
        return keyCache.getActivePrivateKey();
    }

    /**
     * Retrieves a list of RSA public keys from the key cache.
     *
     * @return a list of {@link RSAKey} objects representing the public RSA keys stored in the key cache.
     */
    @Bean
    @Scope("prototype")
    public List<RSAKey> publicKeys() {
        return keyCache.getPublicKeys();
    }

    /**
     * Rotates the active JSON Web Key (JWK) at scheduled intervals.
     * <p>
     * This method is executed every 7 days as defined by the specified cron expression.
     * It deactivates all currently active JWKs, generates a new RSA key,
     * and saves the newly generated key as the active JWK. Additionally,
     * it publishes a {@link JwkKeyRotatedEvent} to signal other components about the rotation.
     * <p>
     * Steps involved in the rotation process:
     * - Fetch all keys from the repository and filter for the active ones.
     * - Mark all active keys as inactive and persist the updates in the repository.
     * - Generate a new RSA key and create a corresponding {@link JwkKey} entity for it.
     * - Save the newly created key entity in the repository and log the rotation process details.
     * - Publish an event to notify other components of the key rotation.
     * <p>
     * Thread safety is ensured by using the synchronized keyword to prevent concurrent execution.
     *
     * @throws IllegalStateException if the generation of the new RSA key fails.
     */
    @Scheduled(cron = "0 0 0 */7 * *")
    public synchronized void rotateKey() {
        log.info("Begin to  rotate JWK key.");
        keyRepo.findAll().stream().filter(JwkKey::isActive).toList().forEach(key -> {
            key.setActive(false);
            keyRepo.save(key);
        });

        try {
            RSAKey newRsaKey = generateRsaKey();
            JwkKey newKeyEntity = generateNewKeyForRotation(newRsaKey);
            keyRepo.save(newKeyEntity);
            log.info("Rotate JWK key successfully. {}", newKeyEntity.toString());

            eventPublisher.publishEvent(new JwkKeyRotatedEvent());
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate new RSA key", e);
        }
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
     *   - Publishes a {@link JwkKeyRotatedEvent} to signal other components about potential key changes.
     * </ul>
     * The method employs synchronization to ensure thread safety during execution.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public synchronized void removeInactiveKeys() {
        log.info("Begin to remove inactive JWK keys.");
        List<JwkKey> allKeys = keyRepo.findAll();
        removedInactiveKeys(allKeys);
        eventPublisher.publishEvent(new JwkKeyRotatedEvent());
    }

    /**
     * Removes inactive JWK keys from the given list of keys based on certain conditions.
     * If the total number of keys in the list exceeds five, this method removes inactive keys
     * that are relatively recent (not older than a specific cutoff time) and may delete the oldest
     * ones first to ensure only a maximum of five keys remain.
     *
     * @param keys A list of {@link JwkKey} objects to evaluate and potentially remove inactive keys from.
     */
    private void removedInactiveKeys(List<JwkKey> keys) {
        Duration tokenLifetime = Duration.ofMinutes(15);
        Instant cutoff = Instant.now().minus(tokenLifetime);

        if (keys.size() > 5) {
            keys.stream()
                    .filter(key -> {
                        Instant keyCreationInstant = key.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant();
                        return !key.isActive() && !keyCreationInstant.isBefore(cutoff);
                    })
                    .sorted(Comparator.comparing(JwkKey::getCreatedDate))
                    .limit(keys.size() - 5L)
                    .forEach(keyRepo::delete);
        }
    }
}