package com.bbmovie.auth.constant.error;

@SuppressWarnings("unused")
public class CloudinaryErrorMessages {
    public static final String MOVIE_NOT_FOUND = "Movie not found with id: %d";
    public static final String MOVIE_NOT_FOUND_BY_PUBLIC_ID = "No movie found with public ID: %s";
    public static final String INVALID_FILE_TYPE = "Invalid file type. Only images and videos are allowed.";
    public static final String FILE_TOO_LARGE = "File size exceeds the maximum limit.";
    public static final String INVALID_FILE_EXTENSION = "Invalid file extension.";

    private CloudinaryErrorMessages() {
    }
}
