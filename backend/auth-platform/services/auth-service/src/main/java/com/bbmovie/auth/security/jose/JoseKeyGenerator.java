package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.jose.JoseKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import java.util.Date;
import java.util.UUID;

public class JoseKeyGenerator {

    private JoseKeyGenerator() {
    }

    public static RSAKey generateRsaKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .algorithm(JWSAlgorithm.RS256)
                .generate();
    }

    public static JoseKey generateNewKeyForRotation(RSAKey rsaKey) {
        return JoseKey.builder()
                .kid(rsaKey.getKeyID())
                .privateJwk(rsaKey.toJSONString())
                .publicJwk(rsaKey.toPublicJWK().toJSONString())
                .isActive(true)
                .build();
    }
}