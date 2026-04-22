package com.bbmovie.transcodeworker.enums;

import lombok.Getter;

import java.util.Set;

/**
 * Enumeration representing the different purposes for file uploads in the system.
 * Each purpose has a specific set of allowed MIME types that are validated during upload.
 */
@Getter
public enum UploadPurpose {
    /** Purpose for user avatar images */
    USER_AVATAR(Set.of("image/jpeg", "image/png")),

    /** Purpose for movie poster images */
    MOVIE_POSTER(Set.of("image/jpeg", "image/png")),

    /** Purpose for movie trailer videos */
    MOVIE_TRAILER(Set.of("video/mp4", "video/webm")),

    /** Purpose for original movie source files that require transcoding */
    MOVIE_SOURCE(Set.of("video/mp4", "video/x-matroska", "video/quicktime"));

    /** Set of allowed MIME types for the specific upload purpose */
    private final Set<String> allowedMimeTypes;

    /**
     * Constructor for UploadPurpose enum values.
     *
     * @param allowedMimeTypes Set of MIME types allowed for this upload purpose
     */
    UploadPurpose(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }
}
