package com.bbmovie.transcodeworker.service.ffmpeg;

import com.bbmovie.transcodeworker.exception.FileUploadException;
import com.bbmovie.transcodeworker.service.ffmpeg.option.CodecOptions;
import com.bbmovie.transcodeworker.service.ffmpeg.option.PresetOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranscoderService {

    private final FFmpeg ffmpeg;
    private final MetadataService metadataService;

    public record VideoResolution(int width, int height, String filename) {
        public String name() {
            return filename; // e.g., "720p", "1080p"
        }
    }

    public record ResolutionDefinition(int minWidth, int targetWidth, int targetHeight, String suffix) { }

    public static final List<ResolutionDefinition> PREDEFINED_RESOLUTIONS = List.of(
            new ResolutionDefinition(1920, 1920, 1080, "1080p"),
            new ResolutionDefinition(1280, 1280, 720, "720p"),
            new ResolutionDefinition(854, 854, 480, "480p"),
            new ResolutionDefinition(640, 640, 360, "360p"),
            new ResolutionDefinition(426, 426, 240, "240p"),
            new ResolutionDefinition(256, 256, 144, "144p")
    );

    public List<VideoResolution> determineTargetResolutions(FFmpegVideoMetadata meta) {
        List<VideoResolution> targets = new ArrayList<>();
        for (ResolutionDefinition def : PREDEFINED_RESOLUTIONS) {
            if (meta.width() >= def.minWidth()) {
                targets.add(new VideoResolution(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }
        // If input is smaller than smallest definition, add original or smallest?
        // For now, if empty, add input resolution? Or simplest mapping.
        if (targets.isEmpty()) {
             targets.add(new VideoResolution(meta.width(), meta.height(), "original"));
        }
        return targets;
    }

    public void transcode(Path input, List<VideoResolution> videoResolutions, String outputDir) {
        FFmpegVideoMetadata meta = metadataService.getMetadata(input);
        
        FFmpegExecutor executor;
        try {
            executor = new FFmpegExecutor(ffmpeg);
        } catch (IOException e) {
            log.error("Failed to initialize FFmpegExecutor: {}", e.getMessage(), e);
            throw new FileUploadException("Unable to process file.");
        }

        Path outputDirPath = Paths.get(outputDir);

        for (VideoResolution res : videoResolutions) {
            try {
                executeHlsTranscodeJob(executor, input, res, outputDirPath, meta);
            } catch (Exception e) {
                log.error("Failed to transcode file {}: {}", input, e.getMessage(), e);
                throw new RuntimeException("Transcoding failed for resolution " + res.filename(), e);
            }
        }

        createMasterPlaylist(videoResolutions, outputDirPath);
    }

    private void executeHlsTranscodeJob(FFmpegExecutor executor, Path input, VideoResolution res, Path outputDir, FFmpegVideoMetadata meta) {
        // Create folder for resolution: /tmp/uuid/720p/
        Path resolutionDir = outputDir.resolve(res.name());
        try {
            Files.createDirectories(resolutionDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory " + resolutionDir, e);
        }

        // Playlist file: /tmp/uuid/720p/playlist.m3u8
        Path playlistPath = resolutionDir.resolve("playlist.m3u8");

        // Segment pattern: /tmp/uuid/720p/seg_%03d.ts
        String segmentFilename = resolutionDir.resolve("seg_%03d.ts").toString();

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(input.toString())
                .overrideOutputFiles(true)
                .addOutput(playlistPath.toString())
                .setVideoCodec(CodecOptions.libx264)
                .setPreset(PresetOptions.veryfast)
                .setAudioCodec("aac")
                .setAudioBitRate(128_000)
                .setVideoFilter("scale=" + res.width() + ":-2")
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", segmentFilename)
                .addExtraArgs("-hls_playlist_type", "vod")
                .done();

        FFmpegJob job = executor.createJob(builder, progress -> {
            if (meta.duration() > 0) {
                double duration_ns = meta.duration() * 1_000_000_000.0;
                double percentage = progress.out_time_ns / duration_ns;
                log.debug("[{}] Transcoding: {}%", res.name(), String.format("%.2f", percentage * 100));
            }
        });
        job.run();
    }

    public void createMasterPlaylist(List<VideoResolution> resolutions, Path outputDir) {
        Path masterPath = outputDir.resolve("master.m3u8");

        StringBuilder content = new StringBuilder();
        content.append("#EXTM3U\n");

        for (VideoResolution res : resolutions) {
            long bandwidth = getEstimatedBandwidth(res);
            content.append(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d\n",
                    bandwidth, res.width(), res.height()));
            content.append(String.format("%s/playlist.m3u8\n", res.name()));
        }

        try {
            Files.writeString(masterPath, content.toString());
            log.info("Generated master playlist at {}", masterPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write master playlist", e);
        }
    }

    private long getEstimatedBandwidth(VideoResolution res) {
        // Simple bitrate estimation logic
        if (res.height() >= 1080) return 6_000_000;
        if (res.height() >= 720) return 3_000_000;
        if (res.height() >= 480) return 1_500_000;
        return 800_000;
    }
}
