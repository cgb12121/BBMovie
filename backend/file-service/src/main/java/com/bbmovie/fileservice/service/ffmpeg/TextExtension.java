package com.bbmovie.fileservice.service.ffmpeg;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum TextExtension {
    TXT("txt"),
    MD("md"),
    JSON("json"),
    XML("xml"),
    CSV("csv");

    private final String extension;

    TextExtension(String extension) {
        this.extension = extension;
    }

    public static List<TextExtension> getAllowedTextExtensions() {
        return Arrays.asList(TextExtension.values());
    }
}