package com.bbmovie.transcodeworker.service.validation.encode;

import com.bbmovie.transcodeworker.service.analysis.AnalysisPersistenceService;
import com.bbmovie.transcodeworker.service.storage.MinioUploadService;
import com.bbmovie.transcodeworker.service.validation.encode.dto.EncodingExpectations;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "app.analysis.vvs", name = "enabled", havingValue = "true")
public class FfprobeEncodeValidationService implements EncodeValidationService {

    private final FFprobe ffprobe;
    private final ObjectMapper objectMapper;
    private final MinioUploadService minioUploadService;
    private final String secureBucket;

    public FfprobeEncodeValidationService(
            FFprobe ffprobe,
            ObjectMapper objectMapper,
            MinioUploadService minioUploadService,
            @Value("${app.minio.secure-bucket}") String secureBucket) {
        this.ffprobe = ffprobe;
        this.objectMapper = objectMapper;
        this.minioUploadService = minioUploadService;
        this.secureBucket = secureBucket;
    }

    @Override
    public ValidationReport validate(
            String uploadId,
            Path encodedFile,
            String renditionSuffix,
            EncodingExpectations expectations) {
        List<String> violations = new ArrayList<>();
        String artifactUri = null;
        try {
            FFmpegProbeResult probeResult = ffprobe.probe(encodedFile.toString());
            artifactUri = uploadFfprobeArtifact(uploadId, renditionSuffix, probeResult);
            Optional<FFmpegStream> videoStreamOpt = probeResult.getStreams().stream()
                    .filter(s -> "video".equalsIgnoreCase(String.valueOf(s.codec_type)))
                    .findFirst();

            if (videoStreamOpt.isEmpty()) {
                return new ValidationReport(
                        ValidationReport.ValidationStatus.FAIL,
                        AnalysisPersistenceService.singleViolation("No video stream found"),
                        artifactUri,
                        Instant.now()
                );
            }

            FFmpegStream video = videoStreamOpt.get();
            if (expectations.expectedCodec() != null && !expectations.expectedCodec().equalsIgnoreCase(video.codec_name)) {
                violations.add("Unexpected video codec: expected=" + expectations.expectedCodec() + ", actual=" + video.codec_name);
            }
            if (expectations.expectedWidth() != null && expectations.expectedWidth() != video.width) {
                violations.add("Unexpected width: expected=" + expectations.expectedWidth() + ", actual=" + video.width);
            }
            if (expectations.expectedHeight() != null && expectations.expectedHeight() != video.height) {
                violations.add("Unexpected height: expected=" + expectations.expectedHeight() + ", actual=" + video.height);
            }
            if (video.bit_rate > 0) {
                if (expectations.minBitrate() != null && video.bit_rate < expectations.minBitrate()) {
                    violations.add("Bitrate below min: min=" + expectations.minBitrate() + ", actual=" + video.bit_rate);
                }
                if (expectations.maxBitrate() != null && video.bit_rate > expectations.maxBitrate()) {
                    violations.add("Bitrate above max: max=" + expectations.maxBitrate() + ", actual=" + video.bit_rate);
                }
            }

            if (expectations.expectedAudioCodec() != null) {
                Optional<FFmpegStream> audioStreamOpt = probeResult.getStreams().stream()
                        .filter(s -> "audio".equalsIgnoreCase(String.valueOf(s.codec_type)))
                        .findFirst();
                if (audioStreamOpt.isEmpty()) {
                    violations.add("Missing audio stream");
                } else if (!expectations.expectedAudioCodec().equalsIgnoreCase(audioStreamOpt.get().codec_name)) {
                    violations.add("Unexpected audio codec: expected=" + expectations.expectedAudioCodec()
                            + ", actual=" + audioStreamOpt.get().codec_name);
                }
            }

            ValidationReport.ValidationStatus status = violations.isEmpty()
                    ? ValidationReport.ValidationStatus.PASS
                    : ValidationReport.ValidationStatus.FAIL;
            return new ValidationReport(status, List.copyOf(violations), artifactUri, Instant.now());
        } catch (Exception e) {
            return new ValidationReport(
                    ValidationReport.ValidationStatus.FAIL,
                    AnalysisPersistenceService.singleViolation("VVS probe error: " + e.getMessage()),
                    artifactUri,
                    Instant.now()
            );
        }
    }

    private String uploadFfprobeArtifact(String uploadId, String renditionSuffix, FFmpegProbeResult probeResult) {
        try {
            String key = "analysis/" + uploadId + "/vvs/" + renditionSuffix + ".ffprobe.json";
            byte[] payload = objectMapper.writeValueAsString(probeResult).getBytes(StandardCharsets.UTF_8);
            minioUploadService.uploadStream(
                    secureBucket,
                    key,
                    new ByteArrayInputStream(payload),
                    payload.length,
                    "application/json"
            );
            return "minio://" + secureBucket + "/" + key;
        } catch (Exception e) {
            return null;
        }
    }
}
