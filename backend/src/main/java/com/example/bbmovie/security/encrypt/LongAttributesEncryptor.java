package com.example.bbmovie.security.encrypt;

import com.example.bbmovie.exception.DataEncryptException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.spec.KeySpec;
import java.util.Base64;

@Log4j2
@Converter()
public class LongAttributesEncryptor implements AttributeConverter<Long, String> {

    @Value("${database.secret-key-password}")
    private String secretKeyPassword = "yourSecretKey";

    @Value("${database.salt}")
    private String salt;

    @Value("${database.iterations}")
    private int iterations;

    @Value("${database.algorithm}")
    private String algorithm;

    @Value("${database.secret-factory.algorithm}")
    private String secretKeyFactoryAlgorithm;

    private SecretKeySpec getKeySpec() {
        try {
            KeySpec spec = new PBEKeySpec(secretKeyPassword.toCharArray(), salt.getBytes(), iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(secretKeyFactoryAlgorithm);
            return new SecretKeySpec(skf.generateSecret(spec).getEncoded(), algorithm);
        } catch (Exception e) {
            log.error("Key generation error: {}", e.getMessage());
            throw new DataEncryptException("Something went wrong with data encryption. Please try again later.");
        }
    }

    @Override
    public String convertToDatabaseColumn(Long id) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            SecretKeySpec keySpec = getKeySpec();
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            String encryptedId = Base64.getEncoder().encodeToString(cipher.doFinal(id.toString().getBytes()));
            log.info("encrypted id: {}", encryptedId);
            return encryptedId;
        } catch (Exception e) {
            log.error("Encryption error: {}", e.getMessage());
            throw new DataEncryptException("Data encryption error");
        }
    }

    @Override
    public Long convertToEntityAttribute(String encodedId) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            SecretKeySpec keySpec = getKeySpec();
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            String decryptedId = new String(cipher.doFinal(Base64.getDecoder().decode(encodedId)));
            log.info("decrypted id: {}", decryptedId);
            return Long.valueOf(decryptedId);
        } catch (Exception e) {
            log.error("Decryption error: {}", e.getMessage());
            throw new DataEncryptException("Data decryption error");
        }
    }
}