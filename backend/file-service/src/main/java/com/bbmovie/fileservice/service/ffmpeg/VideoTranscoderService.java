package com.bbmovie.fileservice.service.ffmpeg;

import com.bbmovie.fileservice.exception.FileUploadException;
import lombok.extern.log4j.Log4j2;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.bbmovie.fileservice.constraints.ResolutionConstraints.*;

@Log4j2
@Service
public class VideoTranscoderService {

    private final FFmpeg ffmpeg;
    private final VideoMetadataService metadataService;

    @Autowired
    public VideoTranscoderService(FFmpeg ffmpeg, VideoMetadataService metadataService) {
        this.ffmpeg = ffmpeg;
        this.metadataService = metadataService;
    }

    public record VideoResolution(int width, int height, String filename) { }

    public record ResolutionDefinition(int minWidth, int targetWidth, int targetHeight, String suffix) { }

    public static final List<ResolutionDefinition> PREDEFINED_RESOLUTIONS = List.of(
            new ResolutionDefinition(1920, 1920, 1080, _1080P),
            new ResolutionDefinition(1280, 1280, 720, _720P),
            new ResolutionDefinition(854, 854, 480, _480P),
            new ResolutionDefinition(640, 640, 360, _360P),
            new ResolutionDefinition(320, 320, 240, _240P),
            new ResolutionDefinition(160, 160, 144, _144P)
    );

    public Mono<List<Path>> transcode(Path input, List<VideoResolution> videoResolutions, String outputDir) {
        return metadataService.getMetadata(input).flatMap(meta -> {
            FFmpegExecutor executor;
            try {
                executor = new FFmpegExecutor(ffmpeg);
            } catch (IOException e) {
                log.error("Failed to initialize FFmpegExecutor: {}", e.getMessage(), e);
                return Mono.error(new FileUploadException("Unable to process file."));
            }

            List<Mono<Path>> transcodingJobs = videoResolutions.stream()
                    .map(res -> createTranscodeJob(executor, input, res, outputDir, meta))
                    .toList();

            return Flux.merge(transcodingJobs)
                    .collectList()
                    .doOnError(throwable ->
                            log.error("Failed to transcode file {}: {}", input, throwable.getMessage(), throwable)
                    );
        });
    }

    private Mono<Path> createTranscodeJob(FFmpegExecutor executor, Path input, VideoResolution res, String outputDir, FFmpegVideoMetadata meta) {
        return Mono.fromCallable(() -> {
            Path outputPath = Paths.get(outputDir, res.filename());

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(input.toString())
                    .overrideOutputFiles(true)
                        .addOutput(outputPath.toString())
                        .setVideoCodec(CodecOptions.libx264)
                        .setPreset(PresetOptions.ultrafast)
                        .setVideoFilter("scale=" + res.width() + ":-2") // -2 preserves an aspect ratio
                        .setFormat("mp4")
                        .done();

            FFmpegJob job = executor.createJob(builder, progress -> {
                if (meta.duration() > 0) {
                    double duration_ns = meta.duration() * 1_000_000_000.0;  // Note: duration is in seconds, out_time_ns is in nanoseconds
                    double percentage = progress.out_time_ns / duration_ns;
                    log.info("Transcoding {}: {}%", res.filename(), percentage * 100);
                }
            });
            job.run();

            log.info("Successfully transcoded {} to {}", input, outputPath);
            return outputPath;
        }).subscribeOn(Schedulers.boundedElastic());
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

                    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
                    FFmpegJob job = executor.createJob(builder, progress -> log.info(progress.toString()));
                    job.run();
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

                    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
                    FFmpegJob job = executor.createJob(builder, progress -> log.info(progress.toString()));
                    job.run();

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
                        throw new IllegalArgumentException("Subtitle file must be in SRT format for stream copying.");
                    }

                    FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(inputVideo.toString())
                                .addInput(subtitleFile.toString())
                                .overrideOutputFiles(true)
                                .addOutput(outputPath.toString())
                                    .setVideoCodec("copy")
                                    .setAudioCodec("copy")
                                    .setSubtitlePreset("mov_text")     // Use mov_text for MP4 container
                                    .addExtraArgs("-map", "0") // Map all streams from video input
                                    .addExtraArgs("-map", "1") // Map all streams from subtitle input
                                    .setFormat("mp4")
                                    .done();

                    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
                    FFmpegJob job = executor.createJob(builder, progress -> log.info(progress.toString()));
                    job.run();

                    return outputPath;
                })
                .doOnError(throwable ->
                        log.error("Failed to add subtitles to {} with subtitle {}: {}", inputVideo, subtitleFile, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Path> addCommentaryWithLoweredAudio(
            Path inputVideo, Path inputAudio, String outputDir, String outputFilename, double volumeReduction
    ) {
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

                    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
                    FFmpegJob job = executor.createJob(builder, progress -> log.info(progress.toString()));
                    job.run();

                    return outputPath;
                })
                .doOnError(throwable ->
                        log.error("Failed to add commentary to {} with audio {}: {}", inputVideo, inputAudio, throwable.getMessage(), throwable)
                )
                .subscribeOn(Schedulers.boundedElastic());
    }
}