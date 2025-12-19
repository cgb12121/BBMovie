package com.bbmovie.transcodeworker.service.ffmpeg.option;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration representing supported video file extensions.
 * Provides constants for common video file formats and utility methods for working with video extensions.
 */
@Getter
public enum VideoExtension {
    /** MP4 (MPEG-4 Part 14) - Most common video container format */
    MP4("mp4"),
    /** MOV (QuickTime File Format) - Apple's video container format */
    MOV("mov"),
    /** AVI (Audio Video Interleave) - Microsoft's video container format */
    AVI("avi"),
    /** MKV (Matroska Video) - Open standard free container format */
    MKV("mkv"),
    /** WMV (Windows Media Video) - Microsoft's compressed video file format */
    WMV("wmv"),
    /** FLV (Flash Video) - Adobe's video format for Flash content */
    FLV("flv"),
    /** WEBM - Open, royalty-free media file format designed for web use */
    WEBM("webm");

    /** The string representation of the file extension */
    private final String extension;

    /**
     * Constructor for VideoExtension enum values.
     *
     * @param extension the file extension string (without the dot)
     */
    VideoExtension(String extension) {
        this.extension = extension;
    }

    /**
     * Returns a list of all supported video extensions.
     *
     * @return a list containing all VideoExtension enum values
     */
    public static List<VideoExtension> getAllowedVideoExtensions() {
        return Arrays.asList(VideoExtension.values());
    }
}
