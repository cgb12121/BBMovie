package com.bbmovie.auth.security.jose.dto;

import com.bbmovie.auth.entity.jose.JoseKey;
import lombok.Getter;

/**
 * Event representing that a JWK (JSON Web Key) rotation or cleanup has occurred.
 * <p>
 * This event is used to signal to interested listeners that they may need to react
 * accordingly, for example, by refreshing cached keys or updating configurations.
 */
@Getter
public class KeyRotatedEvent {

    private final JoseKey newKey;
    private final JoseKey.KeyType keyType;

    /**
     * Event for inactive key cleanup.
     */
    public KeyRotatedEvent() {
        this.newKey = null;
        this.keyType = null;
    }

    /**
     * Event for key rotation.
     *
     * @param newKey The newly generated and saved key entity.
     */
    public KeyRotatedEvent(JoseKey newKey) {
        this.newKey = newKey;
        this.keyType = newKey.getKeyType();
    }
}
