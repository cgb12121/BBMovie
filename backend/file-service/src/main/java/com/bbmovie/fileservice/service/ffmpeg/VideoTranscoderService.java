package com.bbmovie.fileservice.service.ffmpeg;

import lombok.extern.log4j.Log4j2;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.FilenameUtils;
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

    public record VideoResolution(int width, int height, String filename) { }

    public Mono<List<Path>> transcode(Path input, List<VideoResolution> videoResolutions, String outputDir) {
        return Mono.fromCallable(() -> {
                    FFmpegBuilder builder = new FFmpegBuilder()
                            .addProgress(new URI("pipe:1"))
                            .setVerbosity(FFmpegBuilder.Verbosity.INFO)
                            .setInput(input.toString())
                            .overrideOutputFiles(true);

                    StringBuilder filterComplex = new StringBuilder();
                    List<String> outputMappings = new ArrayList<>();

                    filterComplex.append("[0:v]split=").append(videoResolutions.size());
                    for (int i = 0; i < videoResolutions.size(); i++) {
                        filterComplex.append("[v").append(i).append("in]");
                    }
                    filterComplex.append(";");

                    for (int i = 0; i < videoResolutions.size(); i++) {
                        VideoResolution res = videoResolutions.get(i);
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
                    for (int i = 0; i < videoResolutions.size(); i++) {
                        VideoResolution res = videoResolutions.get(i);
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
                    return videoResolutions.stream()
                            .map(res -> Paths.get(outputDir, res.filename))
                            .toList();
                })
                .doOnError(throwable ->
                        log.error("Failed to transcode file {}: {}", input, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Path> removeAudio(Path input, String outputDir, String outputFilename) {
        return Mono.fromCallable(() -> {
                    String sanitizedOutputFilename = FilenameUtils.getName(outputFilename);
                    Path outputPath = Paths.get(outputDir, sanitizedOutputFilename);

                    FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(input.toString())
                            .overrideOutputFiles(true)
                            .addOutput(outputPath.toString())
                            .setVideoCodec("copy")
                            .disableAudio()
                            .setFormat("mp4")
                            .done();

                    new FFmpegExecutor(ffmpeg).createJob(builder).run();
                    return outputPath;
                })
                .doOnError(throwable ->
                        log.error("Failed to remove audio from {}: {}", input, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Path> addAudio(Path inputVideo, Path inputAudio, String outputDir, String outputFilename) {
        return Mono.fromCallable(() -> {
                    String sanitizedOutputFilename = FilenameUtils.getName(outputFilename);
                    Path outputPath = Paths.get(outputDir, sanitizedOutputFilename);

                    FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(inputVideo.toString())
                            .addInput(inputAudio.toString())
                            .overrideOutputFiles(true)
                            .addOutput(outputPath.toString())
                            .setVideoCodec("copy")
                            .setAudioCodec("aac")
                            .addExtraArgs("-map", "0:v:0")
                            .addExtraArgs("-map", "1:a:0")
                            .addExtraArgs("-shortest")
                            .setFormat("mp4")
                            .done();

                    new FFmpegExecutor(ffmpeg).createJob(builder).run();
                    return outputPath;
                })
                .doOnError(throwable ->
                        log.error("Failed to add audio to {} with audio {}: {}", inputVideo, inputAudio, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Path> addSubtitles(Path inputVideo, Path subtitleFile, String outputDir, String outputFilename) {
        return Mono.fromCallable(() -> {
                    String sanitizedOutputFilename = FilenameUtils.getName(outputFilename);
                    Path outputPath = Paths.get(outputDir, sanitizedOutputFilename);

                    String subtitleExt = FilenameUtils.getExtension(subtitleFile.toString());
                    if (!"srt".equalsIgnoreCase(subtitleExt)) {
                        throw new IllegalArgumentException("Subtitle file must be in SRT format");
                    }

                    FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(inputVideo.toString())
                            .overrideOutputFiles(true)
                            .addOutput(outputPath.toString())
                            .setVideoCodec("libx264")
                            .setPreset("ultrafast")
                            .addExtraArgs("-vf", "subtitles=" + subtitleFile.toString())
                            .setFormat("mp4")
                            .done();

                    new FFmpegExecutor(ffmpeg).createJob(builder).run();
                    return outputPath;
                })
                .doOnError(throwable ->
                        log.error("Failed to add subtitles to {} with subtitle {}: {}", inputVideo, subtitleFile, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Path> addCommentaryWithLoweredAudio(Path inputVideo, Path inputAudio, String outputDir, String outputFilename, double volumeReduction) {
        return Mono.fromCallable(() -> {
                    String sanitizedOutputFilename = FilenameUtils.getName(outputFilename);
                    Path outputPath = Paths.get(outputDir, sanitizedOutputFilename);

                    if (volumeReduction < 0.0 || volumeReduction > 1.0) {
                        throw new IllegalArgumentException("Volume reduction must be between 0.0 and 1.0");
                    }

                    FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(inputVideo.toString())
                            .addInput(inputAudio.toString())
                            .overrideOutputFiles(true)
                            .setComplexFilter(
                                    "[0:a]volume=" + volumeReduction + "[lowered];" +
                                            "[lowered][1:a]amix=inputs=2:duration=shortest"
                            )
                            .addOutput(outputPath.toString())
                            .setVideoCodec("copy")
                            .setAudioCodec("aac")
                            .addExtraArgs("-map", "0:v:0")
                            .addExtraArgs("-map", "[amix]")
                            .setFormat("mp4")
                            .done();

                    new FFmpegExecutor(ffmpeg).createJob(builder).run();
                    return outputPath;
                })
                .doOnError(throwable ->
                        log.error("Failed to add commentary to {} with audio {}: {}", inputVideo, inputAudio, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}