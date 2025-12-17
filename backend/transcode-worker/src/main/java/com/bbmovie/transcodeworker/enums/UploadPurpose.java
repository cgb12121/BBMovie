package com.bbmovie.transcodeworker.enums;

import lombok.Getter;

import java.util.Set;

@Getter
public enum UploadPurpose {
    USER_AVATAR(Set.of("image/jpeg", "image/png")),
    MOVIE_POSTER(Set.of("image/jpeg", "image/png")),
    MOVIE_TRAILER(Set.of("video/mp4", "video/webm")),
    MOVIE_SOURCE(Set.of("video/mp4", "video/x-matroska", "video/quicktime"));

    private final Set<String> allowedMimeTypes;

    UploadPurpose(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }
}
