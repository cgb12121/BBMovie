package com.bbmovie.auth.security.jose.config;

import com.bbmovie.auth.entity.jose.JwkKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class RSAKeyService {

    private RSAKeyService() {
    }

    public static RSAKey generateRsaKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .algorithm(JWSAlgorithm.RS256)
                .generate();
    }

    public static JwkKey generateNewKeyForRotation(RSAKey rsaKey) {
        return JwkKey.builder()
                .kid(rsaKey.getKeyID())
                .privateJwk(rsaKey.toJSONString())
                .publicJwk(rsaKey.toPublicJWK().toJSONString())
                .isActive(true)
                .build();
    }
}
