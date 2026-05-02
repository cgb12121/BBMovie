package bbmovie.transcode.vvs.processing;

import bbmovie.transcode.contracts.dto.QualityReportDTO;
import bbmovie.transcode.contracts.dto.ValidationRequest;
import bbmovie.transcode.vvs.config.VvsMediaProcessingProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Post-encode ffprobe checks (ported from {@code ProcessingMediaActivities#validateAndScore} /
 * transcode-worker {@code FfprobeEncodeValidationService} dimension path).
 */
@Slf4j
@RequiredArgsConstructor
public class VvsQualityProcessingService {

    private final MinioClient minioClient;
    private final FFprobe ffprobe;
    private final VvsMediaProcessingProperties properties;

    public QualityReportDTO validateAndScore(ValidationRequest request) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "vvs-" + request.uploadId() + "-");
            Path playlist = workDir.resolve("playlist.m3u8");
            download(properties.getHlsBucket(), request.playlistPath(), playlist);

            FFmpegProbeResult probe = ffprobe.probe(playlist.toString());
            FFmpegStream video = firstVideoStream(probe).orElse(null);
            if (video == null) {
                return new QualityReportDTO(request.renditionLabel(), false, 0, "no_video_stream");
            }
            boolean dimsOk = Math.abs(video.width - request.expectedWidth()) <= 8
                    && Math.abs(video.height - request.expectedHeight()) <= 8;
            double score = dimsOk ? 90.0 : 40.0;
            return new QualityReportDTO(request.renditionLabel(), dimsOk, score, "vvs_ffprobe_dimensions");
        } catch (Exception e) {
            log.warn("VVS validateAndScore failed {}: {}", request.renditionLabel(), e.getMessage());
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

    private void download(String bucket, String key, Path targetPath) throws Exception {
        try (var response = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            Files.copy(response, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Optional<FFmpegStream> firstVideoStream(FFmpegProbeResult probe) {
        if (probe.getStreams() == null) {
            return Optional.empty();
        }
        return probe.getStreams().stream()
                .filter(s -> s.codec_type == FFmpegStream.CodecType.VIDEO)
                .findFirst();
    }
}
