package com.bbmovie.transcodeworker.service.ffmpeg;

/**
 * Record class that represents video metadata extracted from FFmpeg analysis.
 * Contains basic information about a video file such as dimensions, duration, and codec.
 *
 * @param width the width of the video in pixels
 * @param height the height of the video in pixels
 * @param duration the duration of the video in seconds
 * @param codec the codec used for the video stream
 */
public record FFmpegVideoMetadata(int width, int height, double duration, String codec) {
}
