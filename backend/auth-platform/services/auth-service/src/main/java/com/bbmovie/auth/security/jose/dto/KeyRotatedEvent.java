package com.bbmovie.auth.security.jose.dto;

import com.bbmovie.auth.entity.jose.JoseKey;

/**
 * Event representing that a JWK (JSON Web Key) rotation or cleanup has occurred.
 * <p>
 * This event is used to signal to interested listeners that they may need to react
 * accordingly, for example, by refreshing cached keys or updating configurations.
 */
public record KeyRotatedEvent(JoseKey newKey) {
}
