package com.bbmovie.fileservice.utils;

import com.bbmovie.common.enums.EntityType;
import com.bbmovie.fileservice.service.ffmpeg.AudioExtension;
import com.bbmovie.fileservice.service.ffmpeg.ImageExtension;
import com.bbmovie.fileservice.service.ffmpeg.PdfExtension;
import com.bbmovie.fileservice.service.ffmpeg.TextExtension;
import com.bbmovie.fileservice.service.ffmpeg.VideoExtension;

import java.util.List;

public class FileTypeUtils {

    private FileTypeUtils() { }

    /**
     * Determines the entity type based on file extension
     * @param extension the file extension (without the dot)
     * @return the corresponding EntityType
     */
    public static EntityType determineEntityType(String extension) {
        String ext = extension.toLowerCase();
        if (ext.equals("pdf") || ext.equals("doc") || ext.equals("docx") || ext.equals("txt")) return EntityType.DOCUMENT;
        if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg")) return EntityType.IMAGE;
        if (ext.equals("mp3") || ext.equals("wav") || ext.equals("m4a")) return EntityType.AUDIO;
        return EntityType.DOCUMENT; // Default to document for other extensions
    }

    /**
     * Extracts the file extension from the filename
     * @param fileName the filename to extract extension from
     * @return the file extension (without the dot), or empty string if no extension
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Checks if the file is a text-based file by content type or extension
     * @param contentType the MIME content type of the file
     * @param fileExtension the file extension
     * @return true if the file is a text file, false otherwise
     */
    public static boolean isTextFile(String contentType, String fileExtension) {
        List<String> allowedTextExtensions = TextExtension.getAllowedTextExtensions()
                .stream()
                .map(TextExtension::getExtension)
                .toList();

        // Check by content type
        if (contentType.startsWith("text/")) {
            return allowedTextExtensions.contains(fileExtension);
        }

        // Check by specific content types
        if (contentType.contains("application/json") ||
            contentType.contains("application/xml") ||
            contentType.contains("text/csv")) {
            return allowedTextExtensions.contains(fileExtension);
        }

        // Check by extension for markdown files which might not be detected as text/plain
        if ("md".equals(fileExtension)) {
            return allowedTextExtensions.contains(fileExtension);
        }

        return false;
    }

    /**
     * Gets the list of allowed image extensions
     */
    public static List<String> getAllowedImageExtensions() {
        return ImageExtension.getAllowedExtensions()
                .stream()
                .map(ImageExtension::getExtension)
                .toList();
    }

    /**
     * Gets the list of allowed video extensions
     */
    public static List<String> getAllowedVideoExtensions() {
        return VideoExtension.getAllowedVideoExtensions()
                .stream()
                .map(VideoExtension::getExtension)
                .toList();
    }

    /**
     * Gets the list of allowed PDF extensions
     */
    public static List<String> getAllowedPdfExtensions() {
        return PdfExtension.getAllowedPdfExtensions()
                .stream()
                .map(PdfExtension::getExtension)
                .toList();
    }

    /**
     * Gets the list of allowed audio extensions
     */
    public static List<String> getAllowedAudioExtensions() {
        return AudioExtension.getAllowedAudioExtensions()
                .stream()
                .map(AudioExtension::getExtension)
                .toList();
    }

    /**
     * Gets the list of allowed text extensions
     */
    public static List<String> getAllowedTextExtensions() {
        return TextExtension.getAllowedTextExtensions()
                .stream()
                .map(TextExtension::getExtension)
                .toList();
    }
}