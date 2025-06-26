package com.example.bbmovie.controller.admin;

import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;

@Controller
@Log4j2
@SuppressWarnings("all")
@RequestMapping("/ws/jose")
public class JoseDebugController {

    private final RSAKey rsaPrivateKey;
    private final List<RSAKey> rsaPublicKeys;

    @Autowired
    public JoseDebugController(RSAKey rsaPrivateKey, List<RSAKey> rsaPublicKeys) {
        this.rsaPrivateKey = rsaPrivateKey;
        this.rsaPublicKeys = rsaPublicKeys;
    }

    @ResponseBody
    @PostMapping("/decode")
    public ResponseEntity<?> decode(@RequestBody Map<String, String> body) {
        String token = body.get("token");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Token is missing");
        }

        int dotCount = token.split("\\.").length - 1;

        try {
            if (dotCount == 2) {
                // JWS - signed token
                SignedJWT signedJWT = SignedJWT.parse(token);
                String kid = signedJWT.getHeader().getKeyID();

                for (RSAKey key : rsaPublicKeys) {
                    if (Objects.equals(kid, key.getKeyID())) {
                        if (signedJWT.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                            return ResponseEntity.ok(Map.of(
                                    "type", "JWS",
                                    "payload", signedJWT.getPayload().toJSONObject(),
                                    "claims", signedJWT.getJWTClaimsSet().toJSONObject()
                            ));
                        } else {
                            return ResponseEntity.status(401).body("Signature invalid for kid: " + kid);
                        }
                    }
                }
                return ResponseEntity.status(404).body("No matching public key found for kid: " + kid);
            }

            else if (dotCount == 4) {
                // JWE - encrypted token
                EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
                encryptedJWT.decrypt(new RSADecrypter(rsaPrivateKey.toRSAPrivateKey()));

                return ResponseEntity.ok(Map.of(
                        "type", "JWE",
                        "payload", encryptedJWT.getPayload().toJSONObject(),
                        "claims", encryptedJWT.getJWTClaimsSet().toJSONObject()
                ));
            } else {
                return ResponseEntity.badRequest().body("Invalid token format (not JWS or JWE)");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to parse/decode token: " + e.getMessage());
        }
    }

    @MessageMapping("/decode")
    @SendTo("/topic/jwt")
    public Map<?, ?> decodeWebSocket(Map<String, String> body) {
        log.info("Received payload: {}", body);
        String token = body.get("token");

        if (token == null || token.isBlank()) {
            return Map.of("Error", "Token is missing");
        }

        int dotCount = token.split("\\.").length - 1;

        try {
            if (dotCount == 2) {
                // JWS - signed token
                SignedJWT signedJWT = SignedJWT.parse(token);
                String kid = signedJWT.getHeader().getKeyID();

                for (RSAKey key : rsaPublicKeys) {
                    if (Objects.equals(kid, key.getKeyID())) {
                        if (signedJWT.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                            return Map.of(
                                    "type", "JWS",
                                    "payload", signedJWT.getPayload().toJSONObject(),
                                    "claims", signedJWT.getJWTClaimsSet().toJSONObject()
                            );
                        } else {
                            return Map.of("Signature invalid for kid", kid);
                        }
                    }
                }
                return Map.of("No matching public key found for kid", kid);
            }

            else if (dotCount == 4) {
                // JWE - encrypted token
                EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
                encryptedJWT.decrypt(new RSADecrypter(rsaPrivateKey.toRSAPrivateKey()));

                return Map.of(
                        "type", "JWE",
                        "payload", encryptedJWT.getPayload().toJSONObject(),
                        "claims", encryptedJWT.getJWTClaimsSet().toJSONObject()
                );
            } else {
                return Map.of("Error", "Invalid token format (not JWS or JWE)");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return Map.of("Error", "Failed to parse/decode token");
        }
    }
}