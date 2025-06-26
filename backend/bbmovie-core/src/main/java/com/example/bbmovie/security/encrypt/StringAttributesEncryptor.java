package com.example.bbmovie.security.encrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.example.bbmovie.exception.DataEncryptException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;

import java.security.spec.KeySpec;
import java.util.Base64;

@Log4j2
@Converter
public class StringAttributesEncryptor implements AttributeConverter<String, String> {

    @Value("${database.secret-key-password}")
    private String secretKeyPassword;

    @Value("${database.salt}")
    private String salt;

    @Value("${database.iterations}")
    private int iterations;

    @Value("${database.algorithm}")
    private String algorithm;

    @Value("${database.secret-factory.algorithm}")
    private String secretKeyFactoryAlgorithm;

    private SecretKeySpec getKeySpec() {
        KeySpec spec = new PBEKeySpec(secretKeyPassword.toCharArray(), salt.getBytes(), iterations, 256);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(secretKeyFactoryAlgorithm);
            return new SecretKeySpec(skf.generateSecret(spec).getEncoded(), algorithm);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataEncryptException("Key generation error");
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            SecretKeySpec keySpec = getKeySpec();
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            String encryptedData = Base64.getUrlEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));

            log.info("encrypted data: {}", encryptedData);

            return encryptedData;
        } catch (Exception e) {
            log.error("Encryption failed for data [{}]: {} with error: {}", attribute, e, e.getMessage(), e.getCause());
            throw new DataEncryptException("Encryption error");
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            SecretKeySpec keySpec = getKeySpec();
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] decodedBytes = Base64.getUrlDecoder().decode(dbData);
            String decryptedData = new String(cipher.doFinal(decodedBytes));

            log.info("Decrypted data: {}", decryptedData);

            return decryptedData;
        } catch (Exception e) {
            log.error("Decryption failed for data [{}]: {} with error: {}", dbData, e, e.getMessage(), e.getCause());
            throw new DataEncryptException("Decryption error");
        }
    }
}