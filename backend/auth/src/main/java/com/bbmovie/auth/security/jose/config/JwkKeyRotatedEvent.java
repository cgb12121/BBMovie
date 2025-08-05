package com.bbmovie.auth.security.jose.config;

/**
 * Event representing that a JWK (JSON Web Key) key rotation has occurred.
 * <p>
 * This event can be used in systems that use key rotation mechanisms
 * for cryptographic keys, typically in contexts such as JSON Web Tokens (JWT)
 * and security configurations. It signals to the interested listeners or components
 * that the rotation process has been completed, and they may need to react
 * accordingly, for example, by refreshing cached keys or updating configurations.
 * <p>
 * Design Notes:
 *  <p>
 * - This class acts as a simple marker or signal event and does not require
 *   any additional data to be passed during initialization.
 *   <p>
 * - Listeners can subscribe to this event using event-driven mechanisms (e.g.,
 *   Spring's `@EventListener` mechanism).
 */
public class JwkKeyRotatedEvent {
    public JwkKeyRotatedEvent() {
        // No need to pass any values
    }
}