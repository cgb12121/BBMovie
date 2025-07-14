package com.example.bbmovie.security.jose.config;

import com.example.bbmovie.entity.jose.JwkKey;
import com.example.bbmovie.repository.JwkKeyRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;

@Configuration
@EnableScheduling
@Log4j2(topic = "JwkRotation")
public class JwkConfig {

    private final JwkKeyRepository keyRepo;

    @Autowired
    public JwkConfig(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    @Bean
    public RSAKey activePrivateKey() {
        List<JwkKey> activeKeys = keyRepo.findAll().stream().filter(JwkKey::isActive).toList();
        if (activeKeys.isEmpty()) {
            rotateKey();
            activeKeys = keyRepo.findAll().stream().filter(JwkKey::isActive).toList();
        }
        try {
            return RSAKey.parse(activeKeys.get(0).getPrivateJwk());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid active key", e);
        }
    }

    @Bean
    public List<RSAKey> publicKeys() {
        return keyRepo.findAll().stream()
                .sorted(Comparator.comparing(JwkKey::getCreatedAt))
                .map(key -> {
                    try {
                        return RSAKey.parse(key.getPrivateJwk());
                    } catch (Exception e) {
                        throw new IllegalStateException("Invalid key: " + key.getKid(), e);
                    }
                })
                .toList();
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
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate new RSA key", e);
        }
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public synchronized void removeInactiveKeys() {
        log.info("Begin to remove inactive JWK keys.");
        List<JwkKey> allKeys = keyRepo.findAll();
        removedInactiveKeys(allKeys);
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
                .createdAt(Instant.now())
                .build();
    }

    private void removedInactiveKeys(List<JwkKey> keys) {
        Duration tokenLifetime = Duration.ofMinutes(15);
        Instant cutoff = Instant.now().minus(tokenLifetime);
        if (keys.size() > 5) {
            keys.stream()
                    .filter(key -> !key.isActive() && !key.getCreatedAt().isBefore(cutoff))
                    .sorted(Comparator.comparing(JwkKey::getCreatedAt))
                    .limit(keys.size() - 5L)
                    .forEach(keyRepo::delete);
        }
    }
}