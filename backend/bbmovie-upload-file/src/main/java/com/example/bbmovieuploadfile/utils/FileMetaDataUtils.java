package com.example.bbmovieuploadfile.utils;

import com.example.bbmovieuploadfile.service.ffmpeg.FFmpegVideoMetadata;

import static com.example.bbmovieuploadfile.constraints.ResolutionConstraints.*;

public class FileMetaDataUtils {

    private FileMetaDataUtils() { }

    public static String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex != -1 ? filename.substring(lastDotIndex) : "";
    }

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

    public static String getOriginalResolution(FFmpegVideoMetadata meta) {
        int width = meta.width();
        if (width >= 1920) return _1080P;
        if (width >= 1280) return _720P;
        if (width >= 854)  return _480P;
        if (width >= 640)  return _360P;
        if (width >= 320)  return _240P;
        return _144P;
    }

    public static String sanitizeFilenameWithoutExtension(String input) {
        int lastDotIndex = input.lastIndexOf('.');
        String fileNameWithoutExtension = input.substring(0, lastDotIndex);

        return fileNameWithoutExtension
                .replaceAll("[^\\w\\- ]", "_") // Keep only letters, numbers, underscores, hyphens, and spaces
                .replaceAll("\\s+", ""); // Remove all consecutive spaces
    }
}
