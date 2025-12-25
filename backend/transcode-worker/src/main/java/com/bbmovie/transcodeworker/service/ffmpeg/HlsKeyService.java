package com.bbmovie.transcodeworker.service.ffmpeg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.bbmovie.transcodeworker.util.Converter.hexStringToByteArray;

/**
 * Service responsible for HLS encryption key management.
 * <p>
 * Handles:
 * - Generation of encryption key files for HLS segments
 * - Key rotation based on configurable intervals
 * - Creation of FFmpeg key info files
 * <p>
 * Extracted from VideoTranscoderService to follow Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HlsKeyService {

    private final EncryptionService encryptionService;

    /** Base URL for the stream API where keys will be served */
    @Value("${app.transcode.key-server-url}")
    private String streamApiBaseUrl;

    /** Number of segments before rotating encryption keys */
    @Value("${app.transcode.key-rotation-interval:10}")
    private int keyRotationInterval;

    /**
     * Record to store encryption key information for HLS segment encryption.
     *
     * @param index     the sequential index of the key
     * @param filename  the name of the key file
     * @param localPath the local path to the key file
     * @param keyValue  the hexadecimal value of the encryption key
     * @param iv        the initialization vector for encryption
     */
    public record KeyInfo(int index, String filename, Path localPath, String keyValue, String iv) {}

    /**
     * Generates a new master key and IV pair for encryption.
     *
     * @return MasterKeyPair containing the key and IV
     */
    public MasterKeyPair generateMasterKeyPair() {
        String masterKey = encryptionService.generateRandomHex(16);
        String masterIV = encryptionService.generateRandomHex(16);

        log.trace("Generated master encryption - Key: {}..., IV: {}...",
                masterKey.substring(0, 8),
                masterIV.substring(0, 8));

        return new MasterKeyPair(masterKey, masterIV);
    }

    /**
     * Master key pair for HLS encryption.
     */
    public record MasterKeyPair(String key, String iv) {}

    /**
     * Generates encryption key files for HLS segment encryption with rotation.
     * <p>
     * Creates individual key files for groups of segments based on the key rotation interval.
     * Each key is derived from the master key using segment-specific derivation.
     *
     * @param resolutionDir     the directory where key files should be created
     * @param masterKey         the master encryption key used for key derivation
     * @param masterIV          the master initialization vector used for IV derivation
     * @param estimatedSegments the estimated number of segments for the video
     * @return a list of KeyInfo objects containing information about the generated keys
     * @throws IOException if there's an issue creating the key files
     */
    public List<KeyInfo> generateKeyFiles(Path resolutionDir, String masterKey, String masterIV, int estimatedSegments) throws IOException {
        List<KeyInfo> keyInfos = new ArrayList<>();

        int currentKeyIndex = 1;

        for (int segmentIndex = 1; segmentIndex <= estimatedSegments; segmentIndex++) {
            // Rotate to a new key every keyRotationInterval segments
            if ((segmentIndex - 1) % keyRotationInterval == 0) {
                // Create a new key and IV for the next group of segments
                String key = encryptionService.generateKeyForSegment(masterKey, currentKeyIndex);
                String iv = encryptionService.generateIVForSegment(masterIV, currentKeyIndex);

                // Create the actual key file
                String keyFilename = "key_" + currentKeyIndex + ".key";
                Path keyFilePath = resolutionDir.resolve(keyFilename);
                byte[] keyBytes = hexStringToByteArray(key);
                Files.write(keyFilePath, keyBytes);

                // Store key information
                keyInfos.add(new KeyInfo(currentKeyIndex, keyFilename, keyFilePath, key, iv));

                log.debug("Created key file {} for segments {}-{}",
                        keyFilename, segmentIndex,
                        Math.min(segmentIndex + keyRotationInterval - 1, estimatedSegments));

                currentKeyIndex++;
            }
        }

        return keyInfos;
    }

    /**
     * Creates a key info file for FFmpeg to use during HLS encryption.
     * <p>
     * The file contains the public URL, local path, and IV for each encryption key.
     * Format per entry: key_URI\nlocal_key_file_path\nIV_value
     *
     * @param keyInfoPath the path where the key info file should be created
     * @param keyInfos    the list of key information objects
     * @param uploadId    the unique identifier for the upload operation
     * @param resolution  the resolution name for this HLS stream
     * @throws IOException if there's an issue writing the key info file
     */
    public void createKeyInfoFile(Path keyInfoPath, List<KeyInfo> keyInfos, String uploadId, String resolution) throws IOException {
        StringBuilder content = new StringBuilder();

        // PUBLIC URL pattern for key files
        // streamApiBaseUrl = http://localhost:1205/api/stream
        String publicKeyBaseUrl = String.format("%s/keys/%s/%s/", streamApiBaseUrl, uploadId, resolution);

        for (KeyInfo keyInfo : keyInfos) {
            // Format for each entry:
            // 1. Public key URL
            // 2. Local key file path (so FFmpeg can read the key)
            // 3. IV value
            content.append(publicKeyBaseUrl).append(keyInfo.filename()).append("\n")
                    .append(keyInfo.localPath().toAbsolutePath().toString().replace("\\", "/")).append("\n")
                    .append(keyInfo.iv()).append("\n");
        }

        Files.writeString(keyInfoPath, content.toString(), StandardCharsets.UTF_8);
        log.debug("Created key info file with {} keys", keyInfos.size());
    }

    /**
     * Deletes the temporary key info file after transcoding.
     *
     * @param keyInfoPath the path to the key info file
     */
    public void deleteKeyInfoFile(Path keyInfoPath) {
        try {
            Files.deleteIfExists(keyInfoPath);
        } catch (IOException e) {
            log.warn("Failed to delete key info file: {}", keyInfoPath, e);
        }
    }

    /**
     * Returns the key rotation interval.
     */
    public int getKeyRotationInterval() {
        return keyRotationInterval;
    }
}

