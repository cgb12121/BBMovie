package com.example.bbmovie.service.cloudinary;

import com.example.bbmovie.constant.MimeType;
import com.example.bbmovie.exception.FileValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class FileValidator {

    private static final Logger logger = LoggerFactory.getLogger(FileValidator.class);

    private FileValidator() {}

    private static final List<String> ALLOWED_CONTENT_TYPES = MimeType.getAllowedContentTypes();


    private static final Map<String, byte[]> SIMPLE_MAGIC_NUMBERS = Map.of(
            "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[] {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47}
    );

    private static final long MAX_FILE_SIZE_BYTES = (50 * 1024 * 1024); // 50 MB

    public static void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Invalid file: file is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            logger.warn("Invalid file type: {}", contentType);
            throw new FileValidationException("Invalid file type: " + contentType);
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            logger.warn("File size exceeds maximum limit: {} bytes", file.getSize());
            throw new FileValidationException("File size exceeds maximum limit of 50 MB");
        }

        if (!hasValidContent(file, contentType)) {
            logger.warn("Invalid file content for type: {}", contentType);
            throw new FileValidationException("Invalid file content: does not match expected format");
        }
    }

    private static boolean hasValidContent(MultipartFile file, String contentType) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return switch (contentType) {
                case "image/jpeg", "image/png" -> checkSimpleMagicNumber(inputStream, contentType);
                case "image/webp" -> checkWebpMagicNumber(inputStream);
                case "video/mp4" -> checkMp4MagicNumber(inputStream);
                case "video/quicktime" -> checkQuickTimeMagicNumber(inputStream);
                default -> false;
            };
        }
    }

    private static boolean checkSimpleMagicNumber(InputStream inputStream, String contentType) throws IOException {
        byte[] expectedMagic = SIMPLE_MAGIC_NUMBERS.get(contentType);
        if (expectedMagic == null) return false;

        byte[] fileHeader = new byte[expectedMagic.length];
        int bytesRead = inputStream.read(fileHeader);
        if (bytesRead != expectedMagic.length) return false;

        for (int i = 0; i < expectedMagic.length; i++) {
            if (fileHeader[i] != expectedMagic[i]) return false;
        }
        return true;
    }

    private static boolean checkWebpMagicNumber(InputStream inputStream) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 12) return false;
        return header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    }

    private static boolean checkMp4MagicNumber(InputStream inputStream) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 12) return false;
        if (header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
            String brand = new String(header, 8, 4, StandardCharsets.UTF_8);
            return List.of("isom", "mp41", "mp42").contains(brand);
        }
        return false;
    }

    private static boolean checkQuickTimeMagicNumber(InputStream inputStream) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 12) return false;
        return header[4] == 'm' && header[5] == 'o' && header[6] == 'o' && header[7] == 'v';
    }
}