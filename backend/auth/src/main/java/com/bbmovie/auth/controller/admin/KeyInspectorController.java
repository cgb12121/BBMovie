package com.bbmovie.auth.controller.admin;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@RestController
public class KeyInspectorController {

    private final Object activePrivateKeyBean;
    private final Object publicKeysBean;

    @Autowired
    public KeyInspectorController(
            @Qualifier("activePrivateKey") Object activePrivateKey,
            @Qualifier("publicKeys") Object publicKeys) {
        this.activePrivateKeyBean = activePrivateKey;
        this.publicKeysBean = publicKeys;
    }

    @GetMapping("/inspect-keys")
    public String inspectKeys() {
        // Handle activePrivateKey
        RSAKey activeKey;
        if (activePrivateKeyBean instanceof Supplier) {
            activeKey = ((Supplier<RSAKey>) activePrivateKeyBean).get();
        } else {
            activeKey = (RSAKey) activePrivateKeyBean;
        }
        log.info("Active Private Key: kid={}, isActive={}",
                 activeKey.getKeyID(), activeKey.toJSONObject());

        // Handle publicKeys
        List<RSAKey> publicKeys;
        if (publicKeysBean instanceof Supplier) {
           publicKeys = ((Supplier<List<RSAKey>>) publicKeysBean).get();
        } else {
            publicKeys = (List<RSAKey>) publicKeysBean;
        }
        log.info("Public Keys: count={}, keys={}", publicKeys.size(),
                publicKeys.stream().map(RSAKey::getKeyID).toList());

        return "Key values logged. Check application logs.";
    }
}