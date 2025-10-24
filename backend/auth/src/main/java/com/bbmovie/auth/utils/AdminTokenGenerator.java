package com.bbmovie.auth.utils;

import com.bbmovie.auth.entity.jose.JoseKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.UUID;

import java.util.Map;

import static com.example.common.entity.JoseConstraint.JosePayload.*;

public class AdminTokenGenerator {

    private static final Logger log = LoggerFactory.getLogger(AdminTokenGenerator.class);

    public static void main(String[] args) {
        Map<String, String> env = loadEnvFile();

        Map<String, String> properties = Map.of(
                "jakarta.persistence.jdbc.url", env.get("DATABASE_URL"),
                "jakarta.persistence.jdbc.user", env.get("DATABASE_USERNAME"),
                "jakarta.persistence.jdbc.password", env.get("DATABASE_PASSWORD"),
                "jakarta.persistence.jdbc.driver", env.get("DATABASE_DRIVER"),
                "hibernate.dialect", env.get("DATABASE_DIALECT"),
                "hibernate.show_sql", "true"
        );

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("admin-token-generator", properties);

        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();

            // Fetch the latest active JWK
            JoseKey joseKey = em.createQuery(
                    "SELECT k FROM JoseKey k WHERE k.isActive = true ORDER BY k.createdDate DESC",
                            JoseKey.class
                    )
                    .setMaxResults(1)
                    .getSingleResult();

            em.getTransaction().commit();

            String adminToken = generateAdminToken(joseKey);
            log.info("Generated Admin Token:");
            log.info(adminToken);

        } catch (Exception e) {
            log.error("Failed to generate admin token: {}", e.getMessage());
        } finally {
            emf.close();
        }
    }

    private static String generateAdminToken(JoseKey joseKey) throws ParseException, JOSEException {
        JWK jwk = JWK.parse(joseKey.getPrivateJwk());
        RSAKey rsaKey = (RSAKey) jwk;

        JWSSigner signer = new RSASSASigner(rsaKey);

        String iat = String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        String exp = String.valueOf(LocalDateTime.now().plusYears(1).toEpochSecond(ZoneOffset.UTC));

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("admin")
                .issuer("bbmovie-admin-util")
                .claim(ROLE, "ADMIN")
                .claim(IAT, iat)
                .claim(EXP, exp)
                .claim(ABAC.SUBSCRIPTION_TIER, "PREMIUM")
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(joseKey.getKid()).build(),
                claimsSet
        );

        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    public static Map<String, String> loadEnvFile() {
        Map<String, String> envVariables = new HashMap<>();
        Path filePath = Paths.get(System.getProperty("user.dir")) //load root project
                .resolve("auth") //load auth module
                .resolve(".env");
        log.info("Looking for .env file at: {}", filePath);

        if (!Files.exists(filePath)) {
            log.warn(".env file not found at {}", filePath);
            return envVariables;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    envVariables.put(key, value);
                }
            }
            log.info("Loaded {} environment variables from {}", envVariables.size(), filePath);
        } catch (IOException e) {
            log.error("Error reading .env file: {}", e.getMessage(), e);
        }

        return envVariables;
    }
}
