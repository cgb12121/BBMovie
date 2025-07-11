package com.example.bbmovieuploadfile.service.ffmpeg;

import lombok.extern.log4j.Log4j2;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;

@Log4j2
@Service
public class VideoTranscoderService {

    private final FFmpeg ffmpeg;

    @Autowired
    public VideoTranscoderService(FFmpeg ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    public Mono<Path> transcode(Path input, Path output, int width, int height) {
        return Mono.fromCallable(() -> {
                FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(input.toString())
                            .overrideOutputFiles(true)
                            .addOutput(output.toString())
                            .setVideoFilter("scale=" + width + ":" + height)
                            .setVideoCodec("libx264")
                            .setFormat("mp4")
                            .done();

                    new FFmpegExecutor(ffmpeg).createJob(builder).run();
                    return output;
                })
                .doOnError(throwable ->
                        log.error("Failed to transcode file {}: {}", input, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}