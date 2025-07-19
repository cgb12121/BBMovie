package com.bbmovie.fileservice.utils;

public class FileMetaDataUtils {

    private FileMetaDataUtils() { }

    public static String extractLabel(String filename) {
        int firstUnderscoreIndex = filename.indexOf('_');

        int lastDotIndex = filename.lastIndexOf('.');

        if (firstUnderscoreIndex != -1 &&
                lastDotIndex != -1 &&
                firstUnderscoreIndex < lastDotIndex &&
                firstUnderscoreIndex + 1 < lastDotIndex) {

            return filename.substring(firstUnderscoreIndex + 1, lastDotIndex);
        } else {
            return "";
        }
    }

    public static String sanitizeFilenameWithoutExtension(String input) {
        int lastDotIndex = input.lastIndexOf('.');
        String fileNameWithoutExtension = input;
        if (lastDotIndex != -1) {
            fileNameWithoutExtension = input.substring(0, lastDotIndex);
        }

        return fileNameWithoutExtension
                .replaceAll("[^\\w\\- ]", "_") // Keep only letters, numbers, underscores, hyphens, and spaces
                .replaceAll("\\s+", ""); // Remove all consecutive spaces
    }
}
