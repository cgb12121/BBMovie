package com.example.bbmovie.controller.admin;

import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/jose")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class JoseDebugController {

    private final RSAKey rsaPrivateKey;
    private final List<RSAKey> rsaPublicKeys;

    @Autowired
    public JoseDebugController(RSAKey rsaPrivateKey, List<RSAKey> rsaPublicKeys) {
        this.rsaPrivateKey = rsaPrivateKey;
        this.rsaPublicKeys = rsaPublicKeys;
    }

    @PostMapping("/decode")
    @SuppressWarnings("all")
    public ResponseEntity<?> decode(@RequestBody Map<String, String> body) {
        String token = body.get("token");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Token is missing");
        }

        int dotCount = token.split("\\.").length - 1;

        try {
            if (dotCount == 2) {
                SignedJWT signedJWT = SignedJWT.parse(token);
                String kid = signedJWT.getHeader().getKeyID();

                for (RSAKey key : rsaPublicKeys) {
                    if (Objects.equals(kid, key.getKeyID())) {
                        if (signedJWT.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                            return ResponseEntity.ok(Map.of(
                                    "type", "JWS",
                                    "header", signedJWT.getHeader().toJSONObject(),
                                    "payload", signedJWT.getPayload().toJSONObject()
                            ));
                        } else {
                            return ResponseEntity.status(401).body("Signature invalid for kid: " + kid);
                        }
                    }
                }
                return ResponseEntity.status(404).body("No matching public key found for kid: " + kid);
            }

            else if (dotCount == 4) {
                EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
                encryptedJWT.decrypt(new RSADecrypter(rsaPrivateKey.toRSAPrivateKey()));

                return ResponseEntity.ok(Map.of(
                        "type", "JWE",
                        "header", encryptedJWT.getHeader().toJSONObject(),
                        "payload", encryptedJWT.getPayload().toJSONObject()
                ));
            }

            else {
                return ResponseEntity.badRequest().body("Invalid token format (not JWS or JWE)");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to parse/decode token: " + e.getMessage());
        }
    }
}
