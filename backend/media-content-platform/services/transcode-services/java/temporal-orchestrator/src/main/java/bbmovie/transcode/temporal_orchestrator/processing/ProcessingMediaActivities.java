package bbmovie.transcode.temporal_orchestrator.processing;

import bbmovie.transcode.contracts.activity.MediaActivities;
import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.QualityReportDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SubInfo;
import bbmovie.transcode.contracts.dto.SubtitleJsonDTO;
import bbmovie.transcode.contracts.dto.ValidationRequest;
import bbmovie.transcode.temporal_orchestrator.config.MediaProcessingProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;
import org.springframework.util.FileSystemUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class ProcessingMediaActivities implements MediaActivities {

    private final MinioClient minioClient;
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;
    private final MediaProcessingProperties properties;

    @Override
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "analyze-" + uploadId + "-");
            Path source = workDir.resolve("source");
            download(bucket, key, source);

            FFmpegProbeResult probe = ffprobe.probe(source.toString());
            FFmpegStream video = firstVideoStream(probe)
                    .orElseThrow(() -> new IllegalStateException("No video stream in " + bucket + "/" + key));

            double duration = video.duration > 0 ? video.duration
                    : (probe.getFormat() != null && probe.getFormat().duration > 0 ? probe.getFormat().duration : 0.0);
            String codec = video.codec_name != null ? video.codec_name : "unknown";
            return new MetadataDTO(video.width, video.height, duration, codec);
        } catch (Exception e) {
            throw new RuntimeException("analyzeSource failed for " + bucket + "/" + key, e);
        } finally {
            if (workDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(
                    Paths.get(properties.getTempDir()),
                    "encode-" + request.uploadId() + "-" + request.resolution() + "-"
            );
            Path source = workDir.resolve("source");
            download(request.sourceBucket(), request.sourceKey(), source);

            Path outDir = workDir.resolve("hls").resolve(request.resolution());
            Files.createDirectories(outDir);
            Path playlist = outDir.resolve("playlist.m3u8");
            Path segmentPattern = outDir.resolve("seg_%03d.ts");

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            FFmpegBuilder builder = new FFmpegBuilder()
                    .overrideOutputFiles(true)
                    .setInput(source.toString())
                    .done()
                    .addOutput(playlist.toString())
                    .setVideoCodec("libx264")
                    .setPreset("veryfast")
                    .setAudioCodec("aac")
                    .setAudioBitRate(128_000)
                    .setVideoFilter("scale=" + request.width() + ":-2")
                    .setFormat("hls")
                    .addExtraArgs("-hls_time", "10")
                    .addExtraArgs("-hls_list_size", "0")
                    .addExtraArgs("-hls_segment_filename", segmentPattern.toString())
                    .addExtraArgs("-hls_playlist_type", "vod")
                    .addExtraArgs("-threads", "0")
                    .done();

            FFmpegJob job = executor.createJob(builder, progress -> {
                try {
                    Activity.getExecutionContext().heartbeat(progress.out_time_ns);
                } catch (Exception ignored) {
                }
            });
            job.run();

            String prefix = properties.getMoviesKeyPrefix() + "/" + request.uploadId() + "/" + request.resolution() + "/";
            uploadTree(properties.getHlsBucket(), outDir, prefix);

            String objectKey = prefix + "playlist.m3u8";
            return new RungResultDTO(request.resolution(), objectKey, true);
        } catch (Exception e) {
            log.error("encodeResolution failed {}", request.resolution(), e);
            return new RungResultDTO(request.resolution(), "", false);
        } finally {
            if (workDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "vvs-" + request.uploadId() + "-");
            Path playlist = workDir.resolve("playlist.m3u8");
            download(properties.getHlsBucket(), request.playlistPath(), playlist);

            FFmpegProbeResult probe = ffprobe.probe(
                    ffprobe.builder()
                            .setInput(playlist.toString())
                            .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls,crypto")
                            .build());
            FFmpegStream video = firstVideoStream(probe).orElse(null);
            if (video == null) {
                return new QualityReportDTO(request.renditionLabel(), false, 0, "no_video_stream");
            }
            boolean dimsOk = Math.abs(video.width - request.expectedWidth()) <= 8
                    && Math.abs(video.height - request.expectedHeight()) <= 8;
            double score = dimsOk ? 90.0 : 40.0;
            return new QualityReportDTO(request.renditionLabel(), dimsOk, score, "ffprobe_dimensions");
        } catch (Exception e) {
            log.warn("validateAndScore failed {}: {}", request.renditionLabel(), e.getMessage());
            return new QualityReportDTO(request.renditionLabel(), false, 0, e.getMessage());
        } finally {
            if (workDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        Path workDir = null;
        try {
            List<RungResultDTO> ok = rungs.stream()
                    .filter(RungResultDTO::success)
                    .sorted(Comparator.<RungResultDTO>comparingInt(r -> heightFromLabel(r.resolution())).reversed())
                    .toList();
            if (ok.isEmpty()) {
                return new FinalManifestDTO("", false);
            }
            String uploadId = extractUploadIdFromKey(ok.get(0).playlistPath());
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "master-" + uploadId + "-");
            Path masterFile = workDir.resolve("master.m3u8");
            try (BufferedWriter w = Files.newBufferedWriter(masterFile, StandardCharsets.UTF_8)) {
                w.write("#EXTM3U\n");
                w.write("#EXT-X-VERSION:3\n");
                for (RungResultDTO r : ok) {
                    int bw = bandwidthEstimate(r.resolution());
                    w.write("#EXT-X-STREAM-INF:BANDWIDTH=" + bw + ",RESOLUTION=" + widthFromLabel(r.resolution()) + "x"
                            + heightFromLabel(r.resolution()) + "\n");
                    w.write(r.resolution() + "/playlist.m3u8\n");
                }
            }
            String objectKey = properties.getMoviesKeyPrefix() + "/" + uploadId + "/master.m3u8";
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(properties.getHlsBucket())
                            .object(objectKey)
                            .filename(masterFile.toString())
                            .contentType("application/vnd.apple.mpegurl")
                            .build()
            );
            return new FinalManifestDTO(objectKey, true);
        } catch (Exception e) {
            throw new RuntimeException("generateMasterManifest failed", e);
        } finally {
            if (workDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        return new SubtitleJsonDTO(uploadId, "{}");
    }

    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        return new SubtitleJsonDTO(json.uploadId(), json.jsonPayload());
    }

    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        String key = properties.getMoviesKeyPrefix() + "/" + uploadId + "/master.m3u8";
        return new ManifestUpdateDTO(key, true);
    }

    private void download(String bucket, String key, Path targetPath) throws Exception {
        try (var response = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            Files.copy(response, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void uploadTree(String bucket, Path root, String keyPrefix) throws Exception {
        String normalizedPrefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk.filter(Files::isRegularFile).sorted().toList();
            for (Path file : files) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                String objectKey = normalizedPrefix + relative;
                String contentType = guessContentType(file);
                minioClient.uploadObject(
                        UploadObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .filename(file.toString())
                                .contentType(contentType)
                                .build()
                );
            }
        }
    }

    private static String guessContentType(Path file) {
        String n = file.getFileName().toString().toLowerCase();
        if (n.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (n.endsWith(".ts")) {
            return "video/mp2t";
        }
        return "application/octet-stream";
    }

    private static Optional<FFmpegStream> firstVideoStream(FFmpegProbeResult probe) {
        if (probe.getStreams() == null) {
            return Optional.empty();
        }
        return probe.getStreams().stream()
                .filter(s -> s.codec_type == CodecType.VIDEO)
                .findFirst();
    }

    private static int heightFromLabel(String label) {
        if (label == null) {
            return 0;
        }
        return switch (label) {
            case "1080p" -> 1080;
            case "720p" -> 720;
            case "480p" -> 480;
            default -> {
                if (label.endsWith("p")) {
                    try {
                        yield Integer.parseInt(label.substring(0, label.length() - 1));
                    } catch (NumberFormatException e) {
                        yield 0;
                    }
                }
                yield 0;
            }
        };
    }

    private static int widthFromLabel(String label) {
        return switch (label != null ? label : "") {
            case "1080p" -> 1920;
            case "720p" -> 1280;
            case "480p" -> 854;
            default -> widthFromParsedHeight(heightFromLabel(label));
        };
    }

    /** 16:9 display width aligned with ladder encodes (480p keeps 854, not pure 848). */
    private static int widthFromParsedHeight(int heightPx) {
        if (heightPx <= 0) {
            return 854;
        }
        if (heightPx == 480) {
            return 854;
        }
        return Math.max(256, (int) Math.round(heightPx * 16.0 / 9.0));
    }

    private static int bandwidthEstimate(String label) {
        int h = heightFromLabel(label);
        return Math.max(500_000, h * 1500);
    }

    private static String extractUploadIdFromKey(String playlistObjectKey) {
        String[] parts = playlistObjectKey.split("/");
        if (parts.length >= 3) {
            return parts[1];
        }
        return "unknown";
    }
}
