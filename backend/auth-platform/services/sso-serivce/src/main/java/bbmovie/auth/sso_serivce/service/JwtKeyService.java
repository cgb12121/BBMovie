package bbmovie.auth.sso_serivce.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtKeyService {
    private RSAKey activeKey;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            activeKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID("sso-" + UUID.randomUUID())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize RSA keys", ex);
        }
    }

    public RSAKey getActiveKey() {
        return activeKey;
    }

    public Map<String, Object> getPublicJwks() {
        JWKSet set = new JWKSet(activeKey.toPublicJWK());
        return set.toJSONObject();
    }
}
