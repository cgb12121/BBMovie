package com.bbmovie.transcodeworker.service.ffmpeg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HlsKeyService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HlsKeyService")
class HlsKeyServiceTest {

    @Mock
    private EncryptionService encryptionService;

    private HlsKeyService hlsKeyService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        hlsKeyService = new HlsKeyService(encryptionService);
        ReflectionTestUtils.setField(hlsKeyService, "streamApiBaseUrl", "http://localhost:1205/api/stream");
        ReflectionTestUtils.setField(hlsKeyService, "keyRotationInterval", 10);
    }

    @Nested
    @DisplayName("generateMasterKeyPair()")
    class GenerateMasterKeyPairTests {

        @Test
        @DisplayName("should generate key and IV pair")
        void shouldGenerateKeyAndIV() {
            // Given
            when(encryptionService.generateRandomHex(16))
                    .thenReturn("0123456789abcdef")
                    .thenReturn("fedcba9876543210");

            // When
            HlsKeyService.MasterKeyPair keyPair = hlsKeyService.generateMasterKeyPair();

            // Then
            assertThat(keyPair.key()).isEqualTo("0123456789abcdef");
            assertThat(keyPair.iv()).isEqualTo("fedcba9876543210");
        }
    }

    @Nested
    @DisplayName("generateKeyFiles()")
    class GenerateKeyFilesTests {

        @Test
        @DisplayName("should generate one key for segments <= rotation interval")
        void shouldGenerateOneKeyForFewSegments() throws Exception {
            // Given
            when(encryptionService.generateKeyForSegment(anyString(), anyInt()))
                    .thenReturn("abcdef1234567890abcdef1234567890");
            when(encryptionService.generateIVForSegment(anyString(), anyInt()))
                    .thenReturn("1234567890abcdef");

            // When
            List<HlsKeyService.KeyInfo> keys = hlsKeyService.generateKeyFiles(
                    tempDir, "masterKey", "masterIV", 5);

            // Then
            assertThat(keys).hasSize(1);
            assertThat(keys.get(0).index()).isEqualTo(1);
            assertThat(keys.get(0).filename()).isEqualTo("key_1.key");
            assertThat(Files.exists(keys.get(0).localPath())).isTrue();
        }

        @Test
        @DisplayName("should generate multiple keys with rotation interval")
        void shouldGenerateMultipleKeysWithRotation() throws Exception {
            // Given
            when(encryptionService.generateKeyForSegment(anyString(), anyInt()))
                    .thenReturn("abcdef1234567890abcdef1234567890");
            when(encryptionService.generateIVForSegment(anyString(), anyInt()))
                    .thenReturn("1234567890abcdef");

            // When - 25 segments with rotation every 10 = 3 keys
            List<HlsKeyService.KeyInfo> keys = hlsKeyService.generateKeyFiles(
                    tempDir, "masterKey", "masterIV", 25);

            // Then
            assertThat(keys).hasSize(3);
            assertThat(keys.get(0).filename()).isEqualTo("key_1.key");
            assertThat(keys.get(1).filename()).isEqualTo("key_2.key");
            assertThat(keys.get(2).filename()).isEqualTo("key_3.key");
        }

        @Test
        @DisplayName("should create key files with correct content")
        void shouldCreateKeyFilesWithCorrectContent() throws Exception {
            // Given
            String expectedKey = "0123456789abcdef0123456789abcdef";
            when(encryptionService.generateKeyForSegment(anyString(), anyInt()))
                    .thenReturn(expectedKey);
            when(encryptionService.generateIVForSegment(anyString(), anyInt()))
                    .thenReturn("fedcba9876543210");

            // When
            List<HlsKeyService.KeyInfo> keys = hlsKeyService.generateKeyFiles(
                    tempDir, "masterKey", "masterIV", 1);

            // Then
            byte[] content = Files.readAllBytes(keys.get(0).localPath());
            assertThat(content).hasSize(16); // 32 hex chars = 16 bytes
        }
    }

    @Nested
    @DisplayName("createKeyInfoFile()")
    class CreateKeyInfoFileTests {

        @Test
        @DisplayName("should create key info file with correct format")
        void shouldCreateKeyInfoFileWithCorrectFormat() throws Exception {
            // Given
            Path keyPath = tempDir.resolve("key_1.key");
            Files.createFile(keyPath);
            
            List<HlsKeyService.KeyInfo> keyInfos = List.of(
                    new HlsKeyService.KeyInfo(1, "key_1.key", keyPath, "keyvalue", "ivvalue")
            );

            Path keyInfoPath = tempDir.resolve("keyinfo.txt");

            // When
            hlsKeyService.createKeyInfoFile(keyInfoPath, keyInfos, "upload123", "720p");

            // Then
            String content = Files.readString(keyInfoPath);
            assertThat(content).contains("http://localhost:1205/api/stream/keys/upload123/720p/key_1.key");
            assertThat(content).contains(keyPath.toAbsolutePath().toString().replace("\\", "/"));
            assertThat(content).contains("ivvalue");
        }
    }

    @Nested
    @DisplayName("deleteKeyInfoFile()")
    class DeleteKeyInfoFileTests {

        @Test
        @DisplayName("should delete existing file")
        void shouldDeleteExistingFile() throws Exception {
            // Given
            Path keyInfoPath = tempDir.resolve("keyinfo.txt");
            Files.createFile(keyInfoPath);
            assertThat(Files.exists(keyInfoPath)).isTrue();

            // When
            hlsKeyService.deleteKeyInfoFile(keyInfoPath);

            // Then
            assertThat(Files.exists(keyInfoPath)).isFalse();
        }

        @Test
        @DisplayName("should not throw when file doesn't exist")
        void shouldNotThrowWhenFileDoesNotExist() {
            // Given
            Path keyInfoPath = tempDir.resolve("nonexistent.txt");

            // When/Then
            assertThatCode(() -> hlsKeyService.deleteKeyInfoFile(keyInfoPath))
                    .doesNotThrowAnyException();
        }
    }
}

