package com.example.bbmovie.security.jose.jwk;

import com.example.bbmovie.entity.jose.JwkKey;
import com.example.bbmovie.repository.JwkKeyRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;

@Configuration
@EnableScheduling
public class JwkConfig {

    private final JwkKeyRepository keyRepo;

    @Autowired
    public JwkConfig(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    @Bean
    public RSAKey activePrivateKey() {
        List<JwkKey> activeKeys = keyRepo.findAllByActiveTrue();
        if (activeKeys.isEmpty()) {
            rotateKey();
            activeKeys = keyRepo.findAllByActiveTrue();
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
                .map(key -> {
                    try {
                        return RSAKey.parse(key.getPrivateJwk());
                    } catch (Exception e) {
                        throw new IllegalStateException("Invalid key: " + key.getKid(), e);
                    }
                })
                .sorted(Comparator.comparing(key -> key.getIssueTime().toString()))
                .toList();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public synchronized void rotateKey() {
        keyRepo.findAllByActiveTrue().forEach(key -> {
            key.setActive(false);
            keyRepo.save(key);
        });

        try {
            RSAKey newRsaKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
            JwkKey newKeyEntity = JwkKey.builder()
                    .kid(newRsaKey.getKeyID())
                    .privateJwk(newRsaKey.toJSONString())
                    .publicJwk(newRsaKey.toPublicJWK().toJSONString())
                    .active(true)
                    .createdAt(Instant.now())
                    .build();
            keyRepo.save(newKeyEntity);

            // Optional: Remove old keys (e.g., keep last 5)
            List<JwkKey> allKeys = keyRepo.findAll();
            if (allKeys.size() > 5) {
                allKeys.stream()
                        .filter(key -> !key.isActive())
                        .sorted(Comparator.comparing(JwkKey::getCreatedAt))
                        .limit(allKeys.size() - 5L)
                        .forEach(keyRepo::delete);
            }
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate new RSA key", e);
        }
    }
}