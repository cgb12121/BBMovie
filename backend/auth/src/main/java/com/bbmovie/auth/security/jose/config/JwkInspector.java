package com.bbmovie.auth.security.jose.config;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class JwkInspector {

    private final ApplicationContext applicationContext;

    @Autowired
    public JwkInspector(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void inspectBeans() {
        try {
            Object activePrivateKeyBean = applicationContext.getBean("activePrivateKey");
            if (activePrivateKeyBean instanceof Supplier) {
                RSAKey activeKey = ((Supplier<RSAKey>) activePrivateKeyBean).get();
                log.info("Active Private Key: kid={}, isActive={}", 
                         activeKey.getKeyID(), activeKey.toJSONObject());
            } else {
                RSAKey activeKey = (RSAKey) activePrivateKeyBean;
                log.info("Active Private Key: kid={}, isActive={}", 
                         activeKey.getKeyID(), activeKey.toJSONObject());
            }
        } catch (Exception e) {
            log.error("Error inspecting activePrivateKey: {}", e.getMessage());
        }

        try {
            Object publicKeysBean = applicationContext.getBean("publicKeys");
            if (publicKeysBean instanceof Supplier) {
                List<RSAKey> publicKeys = ((Supplier<List<RSAKey>>) publicKeysBean).get();
                log.info("Public Keys: count={}, keys={}", publicKeys.size(), 
                         publicKeys.stream().map(RSAKey::getKeyID).toList());
            } else {
                List<RSAKey> publicKeys = (List<RSAKey>) publicKeysBean;
                log.info("Public Keys: count={}, keys={}", publicKeys.size(), 
                         publicKeys.stream().map(RSAKey::getKeyID).toList());
            }
        } catch (Exception e) {
            log.error("Error inspecting publicKeys: {}", e.getMessage());
        }
    }
}