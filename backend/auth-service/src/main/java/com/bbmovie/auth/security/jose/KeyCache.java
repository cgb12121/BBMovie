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

import java.text.ParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Getter
@Slf4j
@Component
@SuppressWarnings("squid:S3077")
public class KeyCache {

    private volatile RSAKey activeRsaKey;
    private volatile List<RSAKey> publicKeys;

    private final JwkKeyRepository keyRepo;

    @Autowired
    public KeyCache(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void initialize() {
        log.info("Initializing KeyCache. Attempting to load active RSA key from database.");
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
        log.info("Received KeyRotatedEvent, refreshing keys.");
        if (event.newKey() != null) { // Caused by key rotation
            log.info("Key rotation event with new key (kid: {}). Updating active key.", event.newKey().getKid());
            setActiveKey(event.newKey());
        }
        refreshPublicKeys();
    }

    private synchronized void setActiveKey(JoseKey activeKeyEntity) {
        log.info("Setting active RSA key from entity with kid: {}", activeKeyEntity.getKid());
        try {
            this.activeRsaKey = RSAKey.parse(activeKeyEntity.getPrivateJwk());
            log.info("Active key is now RSA (kid: {}), loaded successfully.", activeRsaKey.getKeyID());
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse active RSA key with kid: " + activeKeyEntity.getKid(), e);
        }
    }

    private synchronized void refreshPublicKeys() {
        log.debug("Refreshing public RSA keys from database.");
        this.publicKeys = keyRepo.findAll().stream()
                .filter(key -> key.getPublicJwk() != null)
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