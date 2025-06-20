package com.example.bbmovie.security.jose.jwk;

import com.example.bbmovie.entity.jose.JwkKey;
import com.example.bbmovie.repository.JwkKeyRepository;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwkService {

    private final JwkKeyRepository keyRepo;

    @Autowired
    public JwkService(JwkKeyRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    public Map<String, Object> getAllActiveJwks() {
        List<RSAKey> rsaKeys = getAllActivePublicKeys();
        List<JWK> jwks = rsaKeys.stream()
                .map(RSAKey::toPublicJWK)
                .collect(Collectors.toList());

        return new JWKSet(jwks).toJSONObject();
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
