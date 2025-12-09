package com.bbmovie.fileservice.service.validation.tika;

import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TikaValidationServiceTest {

    @Mock
    private Tika tika;

    private TikaValidationService tikaValidationService;

    @BeforeEach
    void setUp() {
        tikaValidationService = new TikaValidationService(tika);
    }

    @Test
    void shouldValidatePdfFileSuccessfully() throws IOException {
        // Given
        Path path = Path.of("test.pdf");
        when(tika.detect(any(java.io.File.class))).thenReturn("application/pdf");

        // When & Then
        StepVerifier.create(tikaValidationService.validateContentType(path))
                .verifyComplete(); // Should complete without error
    }

    @Test
    void shouldValidateImageFilesSuccessfully() throws IOException {
        // Given
        Path pngPath = Path.of("test.png");
        Path jpgPath = Path.of("test.jpg");
        Path jpegPath = Path.of("test.jpeg");
        
        when(tika.detect(any(java.io.File.class))).thenReturn("image/png");

        // When & Then
        StepVerifier.create(tikaValidationService.validateContentType(pngPath))
                .verifyComplete(); // Should complete without error

        when(tika.detect(any(java.io.File.class))).thenReturn("image/jpeg");
        StepVerifier.create(tikaValidationService.validateContentType(jpgPath))
                .verifyComplete(); // Should complete without error

        StepVerifier.create(tikaValidationService.validateContentType(jpegPath))
                .verifyComplete(); // Should complete without error
    }

    @Test
    void shouldValidateAudioFilesSuccessfully() throws IOException {
        // Given
        Path mp3Path = Path.of("test.mp3");
        Path wavPath = Path.of("test.wav");
        Path m4aPath = Path.of("test.m4a");
        
        when(tika.detect(any(java.io.File.class))).thenReturn("audio/mpeg");

        // When & Then
        StepVerifier.create(tikaValidationService.validateContentType(mp3Path))
                .verifyComplete(); // Should complete without error

        when(tika.detect(any(java.io.File.class))).thenReturn("audio/wav");
        StepVerifier.create(tikaValidationService.validateContentType(wavPath))
                .verifyComplete(); // Should complete without error

        when(tika.detect(any(java.io.File.class))).thenReturn("audio/x-m4a");
        StepVerifier.create(tikaValidationService.validateContentType(m4aPath))
                .verifyComplete(); // Should complete without error
    }

    @Test
    void shouldValidateTextFilesSuccessfully() throws IOException {
        // Given
        Path txtPath = Path.of("test.txt");
        Path jsonPath = Path.of("test.json");
        Path xmlPath = Path.of("test.xml");
        Path csvPath = Path.of("test.csv");
        Path mdPath = Path.of("test.md");
        
        when(tika.detect(any(java.io.File.class))).thenReturn("text/plain");

        // When & Then
        StepVerifier.create(tikaValidationService.validateContentType(txtPath))
                .verifyComplete(); // Should complete without error

        when(tika.detect(any(java.io.File.class))).thenReturn("application/json");
        StepVerifier.create(tikaValidationService.validateContentType(jsonPath))
                .verifyComplete(); // Should complete without error

        when(tika.detect(any(java.io.File.class))).thenReturn("application/xml");
        StepVerifier.create(tikaValidationService.validateContentType(xmlPath))
                .verifyComplete(); // Should complete without error

        when(tika.detect(any(java.io.File.class))).thenReturn("text/csv");
        StepVerifier.create(tikaValidationService.validateContentType(csvPath))
                .verifyComplete(); // Should complete without error

        // Markdown files might not be detected as text/plain by Tika, so using filename extension check
        when(tika.detect(any(java.io.File.class))).thenReturn("text/plain");
        StepVerifier.create(tikaValidationService.validateContentType(mdPath))
                .verifyComplete(); // Should complete without error
    }

    @Test
    void shouldRejectUnsupportedFileType() throws IOException {
        // Given
        Path path = Path.of("test.exe");
        when(tika.detect(any(java.io.File.class))).thenReturn("application/x-dosexec");

        // When & Then
        StepVerifier.create(tikaValidationService.validateContentType(path))
                .expectError()
                .verify();
    }
}