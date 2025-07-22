package com.bbmovie.auth.security.jose.config;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Getter
@Slf4j
@Component
public class JwkKeyCache {

    @SuppressWarnings("squid:S3077")
    private volatile RSAKey activePrivateKey;
    @SuppressWarnings("squid:S3077")
    private volatile List<RSAKey> publicKeys;

    private final JwkKeyRepository keyRepo;

    @Autowired
    public JwkKeyCache(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
        refreshActivePrivateKey();
        refreshPublicKeys();
    }

    @EventListener
    public synchronized void handleKeyRotation(JwkKeyRotatedEvent event) {
        log.info("Received JwkKeyRotatedEvent, refreshing keys: {}", event);
        refreshActivePrivateKey();
        this.publicKeys = refreshPublicKeys();
    }

    private synchronized void refreshActivePrivateKey() {
        List<JwkKey> activeKeys = keyRepo.findAll().stream().filter(JwkKey::isActive).toList();
        if (activeKeys.isEmpty()) {
            // Note: Avoid calling rotateKey() here to prevent recursive event publishing
            throw new IllegalStateException("No active keys found");
        }
        try {
            activePrivateKey = RSAKey.parse(activeKeys.getFirst().getPrivateJwk());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid active key", e);
        }
    }

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