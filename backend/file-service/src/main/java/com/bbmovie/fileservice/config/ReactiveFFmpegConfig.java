package com.bbmovie.fileservice.config;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Configuration
public class ReactiveFFmpegConfig {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${ffprobe.path}")
    private String ffprobePath;

    @Bean
    public FFmpeg ffmpeg() {
        return Mono.fromCallable(() -> new FFmpeg(ffmpegPath))
            .subscribeOn(Schedulers.boundedElastic())
            .block();
    }

    @Bean
    public FFprobe ffprobe() {
        return Mono.fromCallable(() -> new FFprobe(ffprobePath))
            .subscribeOn(Schedulers.boundedElastic())
            .block();
    }
}