package com.bbmovie.ai_assistant_service.utils;

import java.util.List;

public class FileTypeUtils {
    
    public static String extractFileName(String fileUrl) {
        if (fileUrl == null) return "unknown";
        int lastSlash = Math.max(fileUrl.lastIndexOf('/'), fileUrl.lastIndexOf('\\'));
        if (lastSlash != -1) {
            return fileUrl.substring(lastSlash + 1);
        }
        return fileUrl;
    }

    public static String extractFileType(String fileUrl) {
        if (fileUrl == null) return "unknown";
        int lastDot = fileUrl.lastIndexOf('.');
        if (lastDot != -1 && lastDot < fileUrl.length() - 1) {
            return fileUrl.substring(lastDot + 1).toLowerCase();
        }
        return "unknown";
    }

    public static boolean isAudioFile(String fileUrl) {
        if (fileUrl == null) return false;
        String lowerFile = fileUrl.toLowerCase();
        return lowerFile.matches(".*\\.(mp3|wav|ogg|m4a|flac|aac|wma)$");
    }

    public static boolean isImageFile(String fileUrl) {
        if (fileUrl == null) return false;
        String lowerFile = fileUrl.toLowerCase();
        return lowerFile.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|tiff|svg)$");
    }

    public static boolean isDocumentFile(String fileUrl) {
        if (fileUrl == null) return false;
        String lowerFile = fileUrl.toLowerCase();
        return lowerFile.matches(".*\\.(pdf|doc|docx|txt|rtf|odt)$");
    }

    public static boolean hasAudioFile(List<String> fileReferences) {
        if (fileReferences == null || fileReferences.isEmpty()) return false;
        return fileReferences.stream().anyMatch(FileTypeUtils::isAudioFile);
    }

    public static String determineFileContentType(List<String> fileReferences) {
        if (fileReferences == null || fileReferences.isEmpty()) {
            return null;
        }

        // Determine the content type based on the file extensions in the URLs
        String firstFile = fileReferences.getFirst().toLowerCase(); // Use get(0) instead of getFirst() for compatibility
        if (firstFile.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return "IMAGE_URL";
        } else if (firstFile.matches(".*\\.(mp3|wav|ogg|m4a|flac)$")) {
            return "AUDIO_TRANSCRIPT"; // Since audio is transcribed
        } else if (firstFile.matches(".*\\.(pdf|txt)$")) {
            return "DOCUMENT_TEXT";
        } else {
            return "GENERIC_FILE";
        }
    }
}
