package com.bbmovie.fileservice.service.ffmpeg;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum PdfExtension {
    PDF("pdf");

    private final String extension;

    PdfExtension(String extension) {
        this.extension = extension;
    }

    public static List<PdfExtension> getAllowedPdfExtensions() {
        return Arrays.asList(PdfExtension.values());
    }
}