package com.bbmovie.auth.controller.admin;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@Endpoint(id = "keyBeans")
public class KeyBeansEndpoint {

    private final Object activePrivateKeyBean;
    private final Object publicKeysBean;

    @Autowired
    public KeyBeansEndpoint(
            @Qualifier("activePrivateKey") Object activePrivateKey,
            @Qualifier("publicKeys") Object publicKeys
    ) {
        this.activePrivateKeyBean = activePrivateKey;
        this.publicKeysBean = publicKeys;
    }

    @ReadOperation
    public Map<String, Object> getKeyBeans() {
        Map<String, Object> result = new HashMap<>();

        // Handle activePrivateKey
        RSAKey activeKey;
        if (activePrivateKeyBean instanceof Supplier) {
            activeKey = ((Supplier<RSAKey>) activePrivateKeyBean).get();
        } else {
            activeKey = (RSAKey) activePrivateKeyBean;
        }
        result.put("activePrivateKey", Map.of("kid", activeKey.getKeyID(), "jwk", activeKey.toJSONObject()));

        // Handle publicKeys
        List<RSAKey> publicKeys;
        if (publicKeysBean instanceof Supplier) {
            publicKeys = ((Supplier<List<RSAKey>>) publicKeysBean).get();
        } else {
            publicKeys = (List<RSAKey>) publicKeysBean;
        }
        result.put("publicKeys", publicKeys.stream()
                .map(key -> Map.of("kid", key.getKeyID(), "jwk", key.toJSONObject()))
                .toList());

        return result;
    }
}