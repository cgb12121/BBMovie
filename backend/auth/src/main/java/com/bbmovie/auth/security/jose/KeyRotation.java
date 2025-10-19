package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.jose.JoseKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.bbmovie.auth.security.jose.dto.KeyRotatedEvent;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import static com.bbmovie.auth.security.jose.JoseKeyGenerator.generateNewKeyForRotation;
import static com.bbmovie.auth.security.jose.JoseKeyGenerator.generateRsaKey;

@Log4j2
@Configuration
@EnableScheduling
public class KeyRotation {

    private final JwkKeyRepository keyRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public KeyRotation(JwkKeyRepository keyRepo, ApplicationEventPublisher eventPublisher) {
        this.keyRepo = keyRepo;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void initializeKeys() {
        if (keyRepo.count() == 0) {
            log.info("No keys found in the database. Initializing a new RSA key.");
            rotateToNewRsaKey();
        }
    }

    @Scheduled(cron = "0 0 0 */7 * *") // Rotate every 7 days
    public synchronized void rotateToNewRsaKey() {
        log.info("Begin to rotate to a new RSA JWK key.");
        deactivateAllKeys();

        try {
            RSAKey newRsaKey = generateRsaKey();
            JoseKey newKeyEntity = generateNewKeyForRotation(newRsaKey);
            keyRepo.save(newKeyEntity);
            log.info("Rotated to new RSA JWK key successfully. KID: {}", newKeyEntity.getKid());

            eventPublisher.publishEvent(new KeyRotatedEvent(newKeyEntity));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate new RSA key", e);
        }
    }

    private void deactivateAllKeys() {
        keyRepo.findAll().stream().filter(JoseKey::isActive).forEach(key -> {
            key.setActive(false);
            keyRepo.save(key);
        });
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public synchronized void removeInactiveKeys() {
        log.info("Begin to remove inactive JWK keys.");
        List<JoseKey> allKeys = keyRepo.findAll();
        removedInactiveKeys(allKeys);
    }

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