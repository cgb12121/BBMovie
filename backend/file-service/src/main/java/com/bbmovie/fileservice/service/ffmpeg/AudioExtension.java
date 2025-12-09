package com.bbmovie.fileservice.service.ffmpeg;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum AudioExtension {
    MP3("mp3"),
    WAV("wav"),
    M4A("m4a");

    private final String extension;

    AudioExtension(String extension) {
        this.extension = extension;
    }

    public static List<AudioExtension> getAllowedAudioExtensions() {
        return Arrays.asList(AudioExtension.values());
    }
}