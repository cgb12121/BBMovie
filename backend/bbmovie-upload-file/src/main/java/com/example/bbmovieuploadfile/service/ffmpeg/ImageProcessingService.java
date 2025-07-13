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
public class ImageProcessingService {

    private final FFmpeg ffmpeg;

    @Autowired
    public ImageProcessingService(FFmpeg ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    public Mono<Path> processImage(Path input, Path output, int width, int height, ImageExtension format) {
        return Mono.fromCallable(() -> {
                    String outputPath = output.toString();
                    FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(input.toString())
                            .overrideOutputFiles(true)
                            .addOutput(outputPath)
                            .setFormat(format.getExtension())
                            .setVideoFilter("scale=" + width + ":" + height)
                            .addExtraArgs("-q:v", "3")
                            .done();
                    new FFmpegExecutor(ffmpeg).createJob(builder).run();
                    return output;
                })
                .doOnError(err -> log.error("Image processing failed: {}", err.getMessage(), err))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
