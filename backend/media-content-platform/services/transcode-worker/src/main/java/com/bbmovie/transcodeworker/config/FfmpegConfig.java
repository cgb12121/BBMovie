package com.bbmovie.transcodeworker.config;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration class for FFmpeg dependencies.
 * This class provides the necessary beans for FFmpeg and FFprobe clients used in video processing.
 */
@Configuration
public class FfmpegConfig {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:ffprobe}")
    private String ffprobePath;

    /**
     * Creates and returns an FFmpeg bean instance.
     *
     * @return FFmpeg instance configured with the specified path
     * @throws IOException if there's an issue initializing FFmpeg
     */
    @Bean
    public FFmpeg ffmpeg() throws IOException {
        return new FFmpeg(ffmpegPath);
    }

    /**
     * Creates and returns an FFprobe bean instance.
     *
     * @return FFprobe instance configured with the specified path
     * @throws IOException if there's an issue initializing FFprobe
     */
    @Bean
    public FFprobe ffprobe() throws IOException {
        return new FFprobe(ffprobePath);
    }
}
