package com.example.bbmovieuploadfile.service.ffmpeg;
import lombok.extern.log4j.Log4j2;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import net.bramp.ffmpeg.probe.FFmpegStream;

import java.nio.file.Path;

@Log4j2
@Service
public class VideoMetadataService {

    private final FFprobe ffprobe;

    @Autowired
    public VideoMetadataService(FFprobe ffprobe) {
        this.ffprobe = ffprobe;
    }

    public Mono<FFmpegVideoMetadata> getMetadata(Path videoPath) {
        return Mono.fromCallable(() -> ffprobe.probe(videoPath.toString()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(probeResult -> {
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
                });
    }
}