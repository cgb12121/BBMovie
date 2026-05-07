package bbmovie.transcode.vqs.processing;

import bbmovie.transcode.contracts.dto.QualityReportDTO;
import bbmovie.transcode.contracts.dto.ValidationRequest;
import bbmovie.transcode.vqs.config.VqsMediaProcessingProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;
import org.springframework.util.FileSystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Quality scoring service for encoded HLS renditions.
 *
 * <p>The service validates geometry with ffprobe, then runs ffmpeg+libvmaf against the source
 * media reference and returns aggregated VMAF metrics for quality gating.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VqsQualityProcessingService {

    private record VmafAggregate(double mean, double p10, double worstWindow) {
    }

    private final MinioClient minioClient;
    private final FFprobe ffprobe;
    private final VqsMediaProcessingProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates rendition dimensions and scores quality with libvmaf.
     *
     * @param request validation request with rendition path and expected geometry
     * @return quality report with pass/fail and VMAF aggregate metrics
     */
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "vqs-" + request.uploadId() + "-");
            Path playlist = workDir.resolve("playlist.m3u8");
            download(properties.getHlsBucket(), request.playlistPath(), playlist);
            Path source = workDir.resolve("source-reference.mp4");
            download(request.sourceBucket(), request.sourceKey(), source);
            Path vmafJson = workDir.resolve("vmaf-report.json");

            FFmpegProbeResult probe = ffprobe.probe(
                    ffprobe.builder()
                            .setInput(playlist.toString())
                            // Local HLS probe can reference nested segments/keys over mixed schemes.
                            .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls,crypto")
                            .build());
            FFmpegStream video = firstVideoStream(probe).orElse(null);
            if (video == null) {
                return new QualityReportDTO(request.renditionLabel(), false, 0, "no_video_stream",
                        null, null, null, "no_video_stream");
            }
            boolean dimsOk = Math.abs(video.width - request.expectedWidth()) <= 8
                    && Math.abs(video.height - request.expectedHeight()) <= 8;
            if (!dimsOk) {
                return new QualityReportDTO(
                        request.renditionLabel(),
                        false,
                        0,
                        String.format(
                                Locale.ROOT,
                                "dimension_mismatch expected=%dx%d actual=%dx%d",
                                request.expectedWidth(),
                                request.expectedHeight(),
                                video.width,
                                video.height
                        ),
                        null,
                        null,
                        null,
                        "dimension_mismatch"
                );
            }
            if (!properties.isVmafEnabled()) {
                return new QualityReportDTO(
                        request.renditionLabel(),
                        true,
                        0,
                        "vmaf_disabled",
                        null,
                        null,
                        null,
                        "vmaf_disabled"
                );
            }
            runLibvmaf(playlist, source, vmafJson, request.expectedWidth(), request.expectedHeight());
            double fps = parseFps(video.avg_frame_rate).orElse(24.0);
            VmafAggregate metrics = readVmafMetrics(vmafJson, fps);
            boolean meanPass = metrics.mean() >= properties.getVmafPassThresholdMean();
            boolean p10Pass = metrics.p10() >= properties.getVmafPassThresholdP10();
            boolean worstWindowPass = metrics.worstWindow() >= properties.getVmafPassThresholdWorstWindow();
            boolean passed = meanPass && p10Pass && worstWindowPass;
            String detail = String.format(
                    Locale.ROOT,
                    "mean=%.3f/%.3f p10=%.3f/%.3f worstWindow=%.3f/%.3f",
                    metrics.mean(),
                    properties.getVmafPassThresholdMean(),
                    metrics.p10(),
                    properties.getVmafPassThresholdP10(),
                    metrics.worstWindow(),
                    properties.getVmafPassThresholdWorstWindow()
            );
            return new QualityReportDTO(
                    request.renditionLabel(),
                    passed,
                    metrics.mean(),
                    detail,
                    metrics.mean(),
                    metrics.p10(),
                    metrics.worstWindow(),
                    passed ? "quality_gate_passed" : "quality_gate_failed"
            );
        } catch (Exception e) {
            log.warn("VQS validateAndScore failed {}: {}", request.renditionLabel(), e.getMessage());
            String detail = e.getMessage() != null ? e.getMessage() : "vqs_validation_failed";
            String reasonCode = detail.contains("libvmaf_failed")
                    ? "libvmaf_failed"
                    : detail.contains("libvmaf_timeout")
                            ? "libvmaf_timeout"
                            : detail.contains("vmaf_metrics_not_found")
                                    ? "vmaf_parse_failed"
                                    : "vqs_validation_failed";
            return new QualityReportDTO(request.renditionLabel(), false, 0, detail,
                    null, null, null, reasonCode);
        } finally {
            if (workDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void runLibvmaf(Path playlist, Path source, Path reportPath, int width, int height) throws Exception {
        String modelPath = properties.getVmafModelPath() == null ? "" : properties.getVmafModelPath().trim();
        String vmafFilter = modelPath.isEmpty()
                ? String.format(
                        Locale.ROOT,
                        "[0:v]setpts=PTS-STARTPTS[dist];[1:v]scale=%d:%d:flags=bicubic,setpts=PTS-STARTPTS[ref];"
                                + "[dist][ref]libvmaf=log_fmt=json:log_path=%s:n_threads=%d",
                        width,
                        height,
                        reportPath.toAbsolutePath(),
                        properties.getVmafThreads()
                )
                : String.format(
                        Locale.ROOT,
                        "[0:v]setpts=PTS-STARTPTS[dist];[1:v]scale=%d:%d:flags=bicubic,setpts=PTS-STARTPTS[ref];"
                                + "[dist][ref]libvmaf=log_fmt=json:log_path=%s:n_threads=%d:model_path=%s",
                        width,
                        height,
                        reportPath.toAbsolutePath(),
                        properties.getVmafThreads(),
                        modelPath
                );
        List<String> command = new ArrayList<>();
        command.add(properties.getFfmpegPath());
        command.add("-hide_banner");
        command.add("-nostats");
        command.add("-protocol_whitelist");
        command.add("file,http,https,tcp,tls,crypto");
        command.add("-i");
        command.add(playlist.toAbsolutePath().toString());
        command.add("-i");
        command.add(source.toAbsolutePath().toString());
        command.add("-an");
        command.add("-sn");
        command.add("-lavfi");
        command.add(vmafFilter);
        command.add("-f");
        command.add("null");
        command.add("-");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        boolean completed = process.waitFor(properties.getVmafTimeoutSeconds(), TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("libvmaf_timeout after " + Duration.ofSeconds(properties.getVmafTimeoutSeconds()));
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("libvmaf_failed exitCode=" + process.exitValue() + " output=" + output);
        }
    }

    private VmafAggregate readVmafMetrics(Path reportPath, double fps) throws Exception {
        JsonNode root = objectMapper.readTree(reportPath.toFile());
        JsonNode pooledMean = root.path("pooled_metrics").path("vmaf").path("mean");
        double meanFromPooled = pooledMean.isNumber() ? pooledMean.asDouble() : Double.NaN;

        List<Double> frameScores = new ArrayList<>();
        JsonNode frames = root.path("frames");
        if (frames.isArray()) {
            for (JsonNode frame : frames) {
                JsonNode metrics = frame.path("metrics");
                JsonNode vmafNode = metrics.path("vmaf");
                if (!vmafNode.isNumber()) {
                    vmafNode = metrics.path("VMAF_score");
                }
                if (vmafNode.isNumber()) {
                    frameScores.add(vmafNode.asDouble());
                }
            }
        }

        if (frameScores.isEmpty()) {
            JsonNode aggregateMean = root.path("aggregate").path("VMAF_score");
            if (!aggregateMean.isNumber() && !Double.isFinite(meanFromPooled)) {
                throw new IllegalStateException("vmaf_metrics_not_found");
            }
            double mean = Double.isFinite(meanFromPooled) ? meanFromPooled : aggregateMean.asDouble();
            return new VmafAggregate(mean, mean, mean);
        }

        double computedMean = frameScores.stream().mapToDouble(Double::doubleValue).average()
                .orElseThrow(() -> new IllegalStateException("vmaf_metrics_not_found"));
        double mean = Double.isFinite(meanFromPooled) ? meanFromPooled : computedMean;

        List<Double> sorted = new ArrayList<>(frameScores);
        Collections.sort(sorted);
        int p10Index = Math.max(0, (int) Math.floor((sorted.size() - 1) * 0.10));
        double p10 = sorted.get(p10Index);
        int windowFrames = Math.max(1, (int) Math.round(properties.getVmafWorstWindowSeconds() * fps));
        double worstWindow = computeWorstWindow(frameScores, windowFrames);

        return new VmafAggregate(mean, p10, worstWindow);
    }

    private static Optional<Double> parseFps(Object avgFrameRate) {
        if (avgFrameRate == null) {
            return Optional.empty();
        }
        String value = avgFrameRate.toString();
        if (value.isBlank()) {
            return Optional.empty();
        }
        if (!value.contains("/")) {
            try {
                return Optional.of(Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        String[] parts = value.split("/", 2);
        try {
            double numerator = Double.parseDouble(parts[0]);
            double denominator = Double.parseDouble(parts[1]);
            if (denominator == 0.0d) {
                return Optional.empty();
            }
            return Optional.of(numerator / denominator);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static double computeWorstWindow(List<Double> frameScores, int windowSizeFrames) {
        if (frameScores.isEmpty()) {
            throw new IllegalArgumentException("frameScores must not be empty");
        }
        int window = Math.max(1, Math.min(windowSizeFrames, frameScores.size()));
        double sum = 0;
        for (int i = 0; i < window; i++) {
            sum += frameScores.get(i);
        }
        double worst = sum / window;
        for (int i = window; i < frameScores.size(); i++) {
            sum += frameScores.get(i);
            sum -= frameScores.get(i - window);
            worst = Math.min(worst, sum / window);
        }
        return worst;
    }

    /** Downloads playlist object from MinIO to local temp file for ffprobe access. */
    private void download(String bucket, String key, Path targetPath) throws Exception {
        try (var response = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            Files.copy(response, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Returns first video stream from ffprobe output when available. */
    private static Optional<FFmpegStream> firstVideoStream(FFmpegProbeResult probe) {
        if (probe.getStreams() == null) {
            return Optional.empty();
        }
        return probe.getStreams().stream()
                .filter(s -> s.codec_type == CodecType.VIDEO)
                .findFirst();
    }
}
