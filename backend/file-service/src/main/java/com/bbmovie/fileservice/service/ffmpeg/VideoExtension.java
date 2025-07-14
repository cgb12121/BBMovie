package com.bbmovie.fileservice.service.ffmpeg;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum VideoExtension {
    MP4("mp4");

    private final String extension;

    VideoExtension(String extension) {
        this.extension = extension;
    }

    public static List<VideoExtension> getAllowedVideoExtensions() {
        return Arrays.asList(VideoExtension.values());
    }
}
