package com.example.bbmovieuploadfile.service.ffmpeg;

import lombok.extern.log4j.Log4j2;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class VideoTranscoderService {

    private final FFmpeg ffmpeg;

    @Autowired
    public VideoTranscoderService(FFmpeg ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    public Mono<List<Path>> transcode(Path input, List<Resolution> resolutions, String outputDir) {
        return Mono.fromCallable(() -> {
                    FFmpegBuilder builder = new FFmpegBuilder()
                            .addProgress(new URI("pipe:1"))
                            .setVerbosity(FFmpegBuilder.Verbosity.DEBUG)
                            .setInput(input.toString())
                            .overrideOutputFiles(true);

                    StringBuilder filterComplex = new StringBuilder();
                    List<String> outputMappings = new ArrayList<>();

                    filterComplex.append("[0:v]split=").append(resolutions.size());
                    for (int i = 0; i < resolutions.size(); i++) {
                        filterComplex.append("[v").append(i).append("in]");
                    }
                    filterComplex.append(";");

                    for (int i = 0; i < resolutions.size(); i++) {
                        Resolution res = resolutions.get(i);
                        filterComplex.append("[v").append(i).append("in]")
                                .append("scale=").append(res.width).append(":").append(res.height)
                                .append(":force_original_aspect_ratio=decrease,pad=")
                                .append(res.width).append(":").append(res.height)
                                .append(":(ow-iw)/2:(oh-ih)/2[v").append(i).append("];");

                        outputMappings.add("[v" + i + "]");
                    }

                    // Remove trailing semicolon from filter_complex
                    if (!filterComplex.isEmpty()) {
                        filterComplex.setLength(filterComplex.length() - 1);
                    }

                    // Add outputs for each resolution
                    for (int i = 0; i < resolutions.size(); i++) {
                        Resolution res = resolutions.get(i);
                        String outputPath = Paths.get(outputDir, res.filename).toString();
                        builder.addOutput(outputPath)
                                .setVideoCodec(CodecOptions.libx264)
                                .setPreset(PresetOptions.ultrafast)
                                .setFormat("mp4")
                                .addExtraArgs("-map", outputMappings.get(i))
                                .done();
                    }

                    // Set filter_complex if there are outputs
                    if (!filterComplex.isEmpty()) {
                        builder.setComplexFilter(filterComplex.toString());
                    }

                    // Execute FFmpeg job
                    new FFmpegExecutor(ffmpeg).createJob(builder).run();

                    // Return list of output paths
                    return resolutions.stream()
                            .map(res -> Paths.get(outputDir, res.filename))
                            .toList();
                })
                .doOnError(throwable ->
                        log.error("Failed to transcode file {}: {}", input, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }


    public record Resolution(int width, int height, String filename) { }
}