package com.example.bbmovie.security.jose.jwk;

import com.example.bbmovie.entity.jose.JwkKey;
import com.example.bbmovie.repository.JwkKeyRepository;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Collections;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Log4j2
public class JwkService {

    private final JwkKeyRepository keyRepo;

    @Autowired
    public JwkService(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    public Map<String, Object> getJwk() {
        List<RSAKey> rsaKeys = getAllActivePublicKeys();
        if (rsaKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        return rsaKeys.get(0).toPublicJWK().toJSONObject();
    }

    public Map<String, Object> getAllActiveJwks() {
        List<RSAKey> rsaKeys = getAllActivePublicKeys();
        List<JWK> jwks = rsaKeys.stream()
                .map(RSAKey::toPublicJWK)
                .collect(Collectors.toList());

        return new JWKSet(jwks).toJSONObject();
    }

    public List<Map<String, Object>> getAllPublicJwks() {
        return keyRepo.findAll().stream()
                .sorted(Comparator.comparing(JwkKey::getCreatedAt).reversed())
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
