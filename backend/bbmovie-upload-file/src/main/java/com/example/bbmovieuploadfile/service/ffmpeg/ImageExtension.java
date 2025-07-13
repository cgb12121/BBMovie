package com.example.bbmovieuploadfile.service.ffmpeg;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ImageExtension {
    JPG("jpg"),
    PNG("png"),
    WEBP("webp"), // default
    BMP("bmp");

    private final String extension;

    ImageExtension(String extension) {
        this.extension = extension;
    }

    public static List<ImageExtension> getAllowedExtensions() {
        return Arrays.asList(ImageExtension.values());
    }
}