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
        // Only support jpg/jpeg/png as requested (no GIF or other formats)
        return lowerFile.matches(".*\\.(jpg|jpeg|png)$");
    }

    public static boolean isDocumentFile(String fileUrl) {
        if (fileUrl == null) return false;
        String lowerFile = fileUrl.toLowerCase();
        return lowerFile.matches(".*\\.(pdf|doc|docx|txt|rtf|odt)$");
    }

    public static boolean isTextFile(String fileUrl) {
        if (fileUrl == null) return false;
        String lowerFile = fileUrl.toLowerCase();
        // Match text file extensions supported by rust-ai-context-refinery
        // See: rust-ai-context-refinery/src/api/process.rs line 108
        return lowerFile.matches(".*\\.(txt|md|json|xml|csv)$");
    }

    public static boolean hasAudioFile(List<String> fileReferences) {
        if (fileReferences == null || fileReferences.isEmpty()) return false;
        return fileReferences.stream().anyMatch(FileTypeUtils::isAudioFile);
    }

    /**
     * Determines the content type category based on file references.
     * <p>
     * Note: Image types here match isImageFile() - only jpg/jpeg/png are supported.
     * GIF and WebP are not supported for image processing (OCR/vision).
     */
    public static String determineFileContentType(List<String> fileReferences) {
        if (fileReferences == null || fileReferences.isEmpty()) {
            return null;
        }

        // Determine the content type based on the file extensions in the URLs
        String firstFile = fileReferences.getFirst().toLowerCase();
        
        // Only jpg/jpeg/png are supported for image processing (matches isImageFile)
        if (firstFile.matches(".*\\.(jpg|jpeg|png)$")) {
            return "IMAGE_URL";
        } else if (firstFile.matches(".*\\.(mp3|wav|ogg|m4a|flac|aac|wma)$")) {
            return "AUDIO_TRANSCRIPT"; // Since audio is transcribed
        } else if (firstFile.matches(".*\\.(pdf|txt|md|json|xml|csv)$")) {
            return "DOCUMENT_TEXT";
        } else if (firstFile.matches(".*\\.(doc|docx|odt|rtf)$")) {
            return "WORD_DOCUMENT";
        } else {
            return "GENERIC_FILE";
        }
    }

    /**
     * Extracts the file extension from a filename.
     */
    public static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
