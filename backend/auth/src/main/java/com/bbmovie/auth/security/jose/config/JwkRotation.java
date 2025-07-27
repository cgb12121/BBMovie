package com.bbmovie.auth.security.jose.config;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    @Bean
    public RSAKey activePrivateKey() {
        return keyCache.getActivePrivateKey();
    }

    @Bean
    @Scope("prototype")
    public List<RSAKey> publicKeys() {
        return keyCache.getPublicKeys();
    }

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

            List<JwkKey> allKeys = keyRepo.findAll();
            removedInactiveKeys(allKeys);
            eventPublisher.publishEvent(new JwkKeyRotatedEvent());
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate new RSA key", e);
        }
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public synchronized void removeInactiveKeys() {
        log.info("Begin to remove inactive JWK keys.");
        List<JwkKey> allKeys = keyRepo.findAll();
        removedInactiveKeys(allKeys);
        eventPublisher.publishEvent(new JwkKeyRotatedEvent());
    }

    private RSAKey generateRsaKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .algorithm(JWSAlgorithm.RS256)
                .generate();
    }

    private JwkKey generateNewKeyForRotation(RSAKey rsaKey) {
        return JwkKey.builder()
                .kid(rsaKey.getKeyID())
                .privateJwk(rsaKey.toJSONString())
                .publicJwk(rsaKey.toPublicJWK().toJSONString())
                .isActive(true)
                .build();
    }

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