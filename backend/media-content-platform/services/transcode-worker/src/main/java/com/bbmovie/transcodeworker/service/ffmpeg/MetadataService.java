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
     * @throws IllegalStateException if no video stream is found in the file
     */
    public FFmpegVideoMetadata getMetadata(Path videoPath) {
        return getMetadataFromSource(videoPath.toString());
    }

    /**
     * Extracts metadata from a video URL using FFprobe.
     * This method probes directly from a URL (e.g., presigned MinIO URL) without downloading the file.
     * <p>
     * FFprobe supports reading from HTTP/HTTPS URLs directly, making this efficient
     * for metadata extraction without full file transfer.
     *
     * @param url the URL to the video file (supports HTTP/HTTPS)
     * @return an FFmpegVideoMetadata object containing the extracted metadata
     * @throws IllegalStateException if no video stream is found
     * @throws RuntimeException if probing fails
     */
    public FFmpegVideoMetadata getMetadataFromUrl(String url) {
        log.debug("Probing video from URL: {}", url.substring(0, Math.min(url.length(), 100)) + "...");
        return getMetadataFromSource(url);
    }

    /**
     * Internal method to extract metadata from any source (file path or URL).
     *
     * @param source file path or URL
     * @return FFmpegVideoMetadata
     */
    private FFmpegVideoMetadata getMetadataFromSource(String source) {
        try {
            FFmpegProbeResult probeResult = ffprobe.probe(source);

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
            log.error("Failed to get video metadata from source: {}", source, e);
            throw new RuntimeException("Failed to get video metadata", e);
        }
    }
}
