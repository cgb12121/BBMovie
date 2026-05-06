package bbmovie.transcode.temporal_orchestrator.dto;

public enum UploadPurpose {
    USER_AVATAR,
    MOVIE_POSTER,
    MOVIE_TRAILER,
    MOVIE_SOURCE;

    public boolean isVideo() {
        return this == MOVIE_SOURCE || this == MOVIE_TRAILER;
    }
}
