package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.bbmovie.auth.repository.JwkKeyRepository;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for handling operations related to JSON Web Keys (JWKs).
 * <p>
 * This class provides methods for retrieving, filtering, and converting
 * RSA public keys and JWK data. It interacts with the underlying repository
 * to fetch the required key details and ensures proper formatting and validation
 * of the key data.
 */
@Service
@Log4j2
public class JwkService {

    private final JwkKeyRepository keyRepo;

    @Autowired
    public JwkService(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    /**
     * Retrieves the JSON Web Key (JWK) of the first available active RSA public key.
     * <p>
     * The method fetches all active public RSA keys, selects the first one, converts it
     * into a JWK representation, and returns the corresponding key attributes as a map.
     * If there are no active public RSA keys, an empty map is returned.
     *
     * @return a map representing the JSON Web Key (JWK) of the first active RSA public key,
     *         or an empty map if no active keys exist.
     */
    public Map<String, Object> getJwk() {
        List<RSAKey> rsaKeys = getAllActivePublicKeys();
        if (rsaKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        return rsaKeys.getFirst().toPublicJWK().toJSONObject();
    }

    /**
     * Retrieves a JSON representation of all active JSON Web Keys (JWKs).
     * The method gathers all active RSA public keys, converts them into JWKs,
     * and groups them into a JSON Web Key Set (JWKSet) format.
     *
     * @return a map representing the JSON Web Key Set (JWKSet) containing all active JWKs.
     */
    public Map<String, Object> getAllActiveJwks() {
        List<RSAKey> rsaKeys = getAllActivePublicKeys();
        List<JWK> jwks = rsaKeys.stream()
                .map(RSAKey::toPublicJWK)
                .collect(Collectors.toList());

        return new JWKSet(jwks).toJSONObject();
    }

    /**
     * Retrieves a list of public JSON Web Keys (JWKs) from the repository.
     * The method fetches all JWK records, sorts them in descending order of their creation date,
     * limits the result to the five most recent JWKs, and converts them into a JSON-compatible map structure.
     * If any JWK from the database is invalid, an {@link IllegalStateException} is thrown.
     *
     * @return a list of maps, where each map represents a public JWK.
     */
    public List<Map<String, Object>> getAllPublicJwks() {
        return keyRepo.findAll().stream()
                .sorted(Comparator.comparing(JwkKey::getCreatedDate).reversed())
                .limit(5)
                .map(jwk -> {
                    try {
                        log.info("JWK: {}", jwk.toString());
                        return RSAKey.parse(jwk.getPublicJwk()).toJSONObject();
                    } catch (Exception e) {
                        throw new IllegalStateException("Invalid JWK in database", e);
                    }
                })
                .toList();
    }

    /**
     * Retrieves all active public RSA keys stored in the repository.
     * The method filters the keys marked as active, parses their public
     * JWK representation, and transforms them into RSAKey objects.
     *
     * @return a list of {@link RSAKey} instances representing active public RSA keys.
     *         If any key contains an invalid public JWK, an {@link IllegalStateException} is thrown.
     */
    private List<RSAKey> getAllActivePublicKeys() {
        return keyRepo.findAll().stream()
                .filter(JwkKey::isActive)
                .map(k -> {
                    try {
                        return RSAKey.parse(k.getPublicJwk());
                    } catch (Exception e) {
                        throw new IllegalStateException("Invalid public JWK", e);
                    }
                })
                .toList();
    }
}
