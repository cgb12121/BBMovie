package com.bbmovie.transcodeworker.service.ffmpeg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

    private final FFprobe ffprobe;

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
