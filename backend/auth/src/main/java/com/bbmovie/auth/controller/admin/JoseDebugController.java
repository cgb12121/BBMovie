package com.bbmovie.auth.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.AESDecrypter;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.crypto.SecretKey;
import java.util.*;

@Controller
@Log4j2
@SuppressWarnings("all")
@RequestMapping("/ws/jose")
public class JoseDebugController {

    private final RSAKey rsaPrivateKey;
    private final List<RSAKey> rsaPublicKeys;

    @Value("${app.jose.key.secret}")
    private String jwtSecret;

    @Autowired
    public JoseDebugController(RSAKey rsaPrivateKey, List<RSAKey> rsaPublicKeys) {
        this.rsaPrivateKey = rsaPrivateKey;
        this.rsaPublicKeys = rsaPublicKeys;
    }

    @ResponseBody
    @PostMapping("/decode/signature")
    public ResponseEntity<?> decodeSignature(@RequestBody Map<String, Object> body) {
        log.info("Received: {}", body);
        if (body.get("token") == null) {
            return ResponseEntity.badRequest().body("Token is missing");
        }

        String token = (String) body.get("token");
        if (token.isBlank()) {
            return ResponseEntity.badRequest().body("Token is empty");
        }

        try {
            Base64URL[] parts = JOSEObject.split(token);
            log.info("Parsing token parts: {}", Arrays.toString(Arrays.stream(parts)
                    .map(Base64URL::toString)
                    .toArray()));

            // JWS format (3 parts)
            if (parts.length == 3) {
                String headerJson = parts[0].decodeToString();
                log.info("Parsing JWS header: {}", headerJson);
                Map<String, Object> header = new ObjectMapper().readValue(headerJson, Map.class);
                String alg = (String) header.get("alg");
                log.info("JWS Algorithm: {}", alg);

                if (alg == null) {
                    return ResponseEntity.badRequest().body("Token algorithm not specified");
                }

                if (alg.startsWith("HS")) {
                    log.info("Using HMAC algorithm: {}", alg);
                    return decodeHmacToken(token);
                }
                else if (alg.startsWith("RS") || alg.startsWith("PS")) {
                    log.info("Using RSA signature algorithm: {}", alg);
                    return decodeRsaToken(token);
                }
                else {
                    return ResponseEntity.badRequest().body("Unsupported algorithm: " + alg);
                }
            }

            // JWE format (5 parts)
            else if (parts.length == 5) {
                log.info("Processing JWE token");
                return decodeJweToken(token);
            }
            else {
                return ResponseEntity.badRequest().body("Invalid token format. Expected JWS (3 parts) or JWE (5 parts)");
            }
        } catch (Exception e) {
            log.error("Error decoding token:", e);
            return ResponseEntity.status(500).body("Failed to decode token: " + e.getMessage());
        }
    }

    private ResponseEntity<?> decodeJweToken(String token) {
        try {
            // Parse the JWE token
            EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);

            // Get the key encryption algorithm from header
            JWEHeader header = encryptedJWT.getHeader();
            JWEAlgorithm alg = header.getAlgorithm();
            log.info("JWE Algorithm: {}", alg);

            // Decrypt using the appropriate decrypter
            if (alg.equals(JWEAlgorithm.RSA_OAEP) || alg.equals(JWEAlgorithm.RSA_OAEP_256)) {
                // Use RSA decryption
                encryptedJWT.decrypt(new RSADecrypter(rsaPrivateKey.toRSAPrivateKey()));
            } else if (alg.equals(JWEAlgorithm.DIR)) {
                // Direct encryption with shared symmetric key
                encryptedJWT.decrypt(new AESDecrypter(jwtSecret.getBytes()));
            } else {
                return ResponseEntity.badRequest().body("Unsupported JWE algorithm: " + alg);
            }

            // Build response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("header", header.toJSONObject());
            result.put("encrypted_key", encryptedJWT.getEncryptedKey() != null ?
                    Base64.getEncoder().encodeToString(encryptedJWT.getEncryptedKey().decode()) : null);
            result.put("iv", encryptedJWT.getIV() != null ?
                    Base64.getEncoder().encodeToString(encryptedJWT.getIV().decode()) : null);
            result.put("ciphertext", Base64.getEncoder().encodeToString(encryptedJWT.getCipherText().decode()));
            result.put("auth_tag", encryptedJWT.getAuthTag() != null ?
                    Base64.getEncoder().encodeToString(encryptedJWT.getAuthTag().decode()) : null);
            result.put("decrypted_payload", encryptedJWT.getJWTClaimsSet().toJSONObject());
            result.put("algorithm", alg.toString());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("JWE token decryption error:", e);
            return ResponseEntity.status(500).body("JWE token decryption failed: " + e.getMessage());
        }
    }

    private ResponseEntity<?> decodeHmacToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            if (!signedJWT.verify(new MACVerifier(secretKey))) {
                return ResponseEntity.status(401).body("HMAC signature verification failed");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("header", signedJWT.getHeader().toJSONObject());
            result.put("payload", signedJWT.getJWTClaimsSet().toJSONObject());
            result.put("signature", Base64.getEncoder().encodeToString(signedJWT.getSignature().decode()));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("HMAC token decoding error:", e);
            return ResponseEntity.status(500).body("HMAC token decoding failed: " + e.getMessage());
        }
    }

    private ResponseEntity<?> decodeRsaToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String kid = signedJWT.getHeader().getKeyID();

            for (RSAKey key : rsaPublicKeys) {
                if (Objects.equals(kid, key.getKeyID())) {
                    if (signedJWT.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("header", signedJWT.getHeader().toJSONObject());
                        result.put("payload", signedJWT.getJWTClaimsSet().toJSONObject());
                        result.put("signature", Base64.getEncoder().encodeToString(signedJWT.getSignature().decode()));
                        result.put("algorithm", signedJWT.getHeader().getAlgorithm().toString());
                        result.put("key_id", kid);

                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.status(401).body("RSA signature invalid for kid: " + kid);
                    }
                }
            }
            return ResponseEntity.status(404).body("No matching public key found for kid: " + kid);
        } catch (Exception e) {
            log.error("RSA token decoding error:", e);
            return ResponseEntity.status(500).body("RSA token decoding failed: " + e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/decode/jwk")
    //TODO: fix runtime switch strategy lead to verify valid token all result in error
    public ResponseEntity<?> decode(@RequestBody Map<String, String> body) {
        log.info("Received payload: {}", body);
        log.info("private key: {}", rsaPrivateKey.toString());
        log.info("private keys: {}", rsaPublicKeys.stream().map(s -> toString()).toArray().toString());
        String token = body.get("token");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Token is missing");
        }

        int dotCount = token.split("\\.").length - 1;

        //TODO: hmac, rsa does not compatiable
        try {
            if (dotCount == 2) {
                // JWS - signed token
                SignedJWT signedJWT = SignedJWT.parse(token);
                String kid = signedJWT.getHeader().getKeyID();

                for (RSAKey key : rsaPublicKeys) {
                    if (Objects.equals(kid, key.getKeyID())) {
                        if (signedJWT.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("header", signedJWT.getHeader().toJSONObject());
                            result.put("payload", signedJWT.getJWTClaimsSet().toJSONObject());
                            result.put("signature", signedJWT.getSignature().toJSONString());
                            return ResponseEntity.ok(result);
                        } else {
                            return ResponseEntity.status(401).body("Signature invalid for kid: " + kid);
                        }
                    }
                }
                return ResponseEntity.status(404).body("No matching public key found for kid: " + kid);
            }

            //TODO: jwe nimbus is not compatible
            else if (dotCount == 4) {
                log.debug("Using private key with modulus: {}", rsaPrivateKey.getModulus());
                // JWE - encrypted token
                EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
                encryptedJWT.decrypt(new RSADecrypter(rsaPrivateKey.toRSAPrivateKey()));
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("header", encryptedJWT.getHeader().toJSONObject());
                result.put("encypted_key", encryptedJWT.getEncryptedKey().toJSONString());
                result.put("iv", encryptedJWT.getIV().toJSONString());
                result.put("claims", encryptedJWT.getJWTClaimsSet().toJSONObject());
                result.put("auth_tag", encryptedJWT.getAuthTag().toJSONString());

                return ResponseEntity.ok((result));
            } else {
                return ResponseEntity.badRequest().body("Invalid token format (not JWS or JWE)");
            }
        } catch (Exception e) {
            log.error("Error: {}, {}", e, e.getMessage());
            return ResponseEntity.status(500).body("Failed to parse/decode token: " + e.getMessage());
        }
    }
}