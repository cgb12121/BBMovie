package com.bbmovie.transcodeworker.service.ffmpeg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Service class for extracting metadata from video files using FFprobe.
 * Provides methods to analyze video files and retrieve key information such as dimensions, duration, and codec.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

    /** FFprobe instance used for video metadata extraction */
    private final FFprobe ffprobe;

    /**
     * Extracts metadata from a video file using FFprobe.
     * This method analyzes the video file and returns information about its dimensions, duration, and codec.
     *
     * @param videoPath the path to the video file to analyze
     * @return an FFmpegVideoMetadata object containing the extracted metadata
     * @throws IOException if there's an issue reading the video file
     * @throws IllegalStateException if no video stream is found in the file
     */
    public FFmpegVideoMetadata getMetadata(Path videoPath) {
        try {
            FFmpegProbeResult probeResult = ffprobe.probe(videoPath.toString());

            FFmpegStream videoStream = probeResult.getStreams()
                    .stream()
                    .filter(s -> "video".equalsIgnoreCase(String.valueOf(s.codec_type)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No video stream found"));

            return new FFmpegVideoMetadata(
                    videoStream.width,
                    videoStream.height,
                    probeResult.getFormat().duration,
                    videoStream.codec_name
            );
        } catch (IOException e) {
            log.error("Failed to get video metadata", e);
            throw new RuntimeException("Failed to get video metadata", e);
        }
    }
}
