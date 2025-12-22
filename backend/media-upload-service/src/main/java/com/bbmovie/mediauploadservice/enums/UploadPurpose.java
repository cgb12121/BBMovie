package com.bbmovie.mediauploadservice.enums;

import lombok.Getter;

import java.util.Set;

@Getter
public enum UploadPurpose {
    USER_AVATAR(Set.of("image/jpeg", "image/png")),
    MOVIE_POSTER(Set.of("image/jpeg", "image/png")),
    MOVIE_TRAILER(Set.of("video/mp4", "video/webm")),
    MOVIE_SOURCE(Set.of("video/mp4", "video/x-matroska", "video/quicktime")),
    // AI_ASSET supports all file types that rust-ai-context-refinery can process:
    // - Audio: mp3, wav, m4a
    // - Images: png, jpg, jpeg (for OCR and vision)
    // - PDF: pdf
    // - Text: txt, md, json, xml, csv
    AI_ASSET(Set.of(
            // Audio formats
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/mp4", "audio/m4a",
            // Image formats
            "image/png", "image/jpeg", "image/jpg",
            // PDF
            "application/pdf",
            // Text formats
            "text/plain", "text/markdown", "application/json", "application/xml", "text/xml", "text/csv"
    ));
    
    private final Set<String> allowedMimeTypes;

    UploadPurpose(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }
}
