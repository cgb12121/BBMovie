package com.bbmovie.mediauploadservice.service;

import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import com.bbmovie.mediauploadservice.exception.UnsupportedFileTypeException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

@Component
public class ObjectKeyStrategy {

    public String build(UploadPurpose purpose, String uploadId, String filename) {
        String ext = FilenameUtils.getExtension(filename);
        if (ext == null || ext.isEmpty()) {
            throw new UnsupportedFileTypeException("Invalid file type");
        }

        return switch (purpose) {
            case USER_AVATAR -> "users/avatars/%s.%s".formatted(uploadId, ext);
            case MOVIE_POSTER -> "movies/posters/%s.%s".formatted(uploadId, ext);
            case MOVIE_TRAILER -> "movies/trailers/%s/%s".formatted(uploadId, filename);
            case MOVIE_SOURCE -> "movies/sources/%s/%s".formatted(uploadId, filename);
            case AI_ASSET -> "ai/assets/%s/%s".formatted(uploadId, filename);
        };
    }
}
