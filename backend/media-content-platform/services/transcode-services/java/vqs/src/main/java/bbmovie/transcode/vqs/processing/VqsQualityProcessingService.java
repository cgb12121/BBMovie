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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Quality scoring path (ported from {@code ProcessingMediaActivities#validateAndScore}).
 * VMAF / libvmaf from legacy worker is not wired yet; dimension check runs and score uses a stub when dimensions match.
 */
@Slf4j
@RequiredArgsConstructor
public class VqsQualityProcessingService {

    private final MinioClient minioClient;
    private final FFprobe ffprobe;
    private final VqsMediaProcessingProperties properties;

    /**
     * Validates rendition dimensions and emits placeholder quality score pending libvmaf wiring.
     *
     * @param request validation request with rendition path and expected geometry
     * @return quality report with pass/fail and temporary numeric score
     */
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "vqs-" + request.uploadId() + "-");
            Path playlist = workDir.resolve("playlist.m3u8");
            download(properties.getHlsBucket(), request.playlistPath(), playlist);

            FFmpegProbeResult probe = ffprobe.probe(
                    ffprobe.builder()
                            .setInput(playlist.toString())
                            // Local HLS probe can reference nested segments/keys over mixed schemes.
                            .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls,crypto")
                            .build());
            FFmpegStream video = firstVideoStream(probe).orElse(null);
            if (video == null) {
                return new QualityReportDTO(request.renditionLabel(), false, 0, "no_video_stream");
            }
            boolean dimsOk = Math.abs(video.width - request.expectedWidth()) <= 8
                    && Math.abs(video.height - request.expectedHeight()) <= 8;
            // Keep a deterministic placeholder score until libvmaf wiring is enabled.
            double score = dimsOk ? 92.0 : 35.0;
            log.debug("[vqs] VMAF not run; stub score={} for {}", score, request.renditionLabel());
            return new QualityReportDTO(request.renditionLabel(), dimsOk, score, "vqs_ffprobe_dimensions_vmaf_stub");
        } catch (Exception e) {
            log.warn("VQS validateAndScore failed {}: {}", request.renditionLabel(), e.getMessage());
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
