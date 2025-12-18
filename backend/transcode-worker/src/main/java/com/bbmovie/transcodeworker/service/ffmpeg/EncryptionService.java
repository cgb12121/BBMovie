package com.bbmovie.transcodeworker.service.ffmpeg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import static com.bbmovie.transcodeworker.util.Converter.bytesToHex;
import static com.bbmovie.transcodeworker.util.Converter.hexStringToByteArray;

@Slf4j
@Service
public class EncryptionService {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a random hex string
     */
    public String generateRandomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    /**
     * Generate unique key for a segment based on master key and segment index
     */
    public String generateKeyForSegment(String masterKey, int segmentIndex) {
        try {
            // Use HKDF to derive a unique key for each segment
            byte[] masterKeyBytes = hexStringToByteArray(masterKey);
            byte[] info = ("segment-key-" + segmentIndex).getBytes(StandardCharsets.UTF_8);

            // Simple key derivation using HMAC-SHA256
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "HmacSHA256");
            hmac.init(keySpec);

            byte[] derived = hmac.doFinal(info);
            // Take the first 16 bytes for an AES-128 key
            return bytesToHex(Arrays.copyOfRange(derived, 0, 16));

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key for segment", e);
        }
    }

    /**
     * Generate unique IV for segment
     */
    public String generateIVForSegment(String masterIV, int segmentIndex) {
        try {
            // Combine master IV with segment index for unique IV
            byte[] masterIVBytes = hexStringToByteArray(masterIV);
            ByteBuffer buffer = ByteBuffer.allocate(masterIVBytes.length + 4);
            buffer.put(masterIVBytes);
            buffer.putInt(segmentIndex);

            // Hash to get unique IV
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(buffer.array());

            // Take the first 16 bytes for IV
            return bytesToHex(Arrays.copyOfRange(hash, 0, 16));

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate IV for segment", e);
        }
    }
}
