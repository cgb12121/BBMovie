package com.bbmovie.transcodeworker.service.quality;

import com.bbmovie.transcodeworker.service.storage.MinioUploadService;
import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "app.analysis.vqs", name = "enabled", havingValue = "true")
public class FfmpegVideoQualityService implements VideoQualityService {

    private final ObjectMapper objectMapper;
    private final MinioUploadService minioUploadService;
    private final String ffmpegPath;
    private final String tempDir;
    private final String secureBucket;

    public FfmpegVideoQualityService(
            ObjectMapper objectMapper,
            MinioUploadService minioUploadService,
            @Value("${ffmpeg.path:ffmpeg}") String ffmpegPath,
            @Value("${app.transcode.temp-dir:${java.io.tmpdir}}") String tempDir,
            @Value("${app.minio.secure-bucket}") String secureBucket) {
        this.objectMapper = objectMapper;
        this.minioUploadService = minioUploadService;
        this.ffmpegPath = ffmpegPath;
        this.tempDir = tempDir;
        this.secureBucket = secureBucket;
    }

    @Override
    public QualityReport score(String uploadId, Path sourceFile, Path encodedFile, String renditionSuffix) {
        Path logPath = null;
        try {
            Files.createDirectories(Path.of(tempDir));
            logPath = Files.createTempFile(Path.of(tempDir), "vmaf_", ".json");

            List<String> command = List.of(
                    ffmpegPath,
                    "-i", sourceFile.toString(),
                    "-i", encodedFile.toString(),
                    "-lavfi", "libvmaf=log_fmt=json:log_path=" + logPath + ":feature=name=psnr|name=float_ssim",
                    "-f", "null",
                    "-"
            );

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = readAll(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return new QualityReport(
                        renditionSuffix,
                        "VMAF",
                        0.0,
                        null,
                        null,
                        false,
                        null,
                        "ffmpeg/libvmaf failed: " + output,
                        Instant.now()
                );
            }

            JsonNode root = objectMapper.readTree(logPath.toFile());
            JsonNode meanNode = root.path("pooled_metrics").path("vmaf").path("mean");
            JsonNode psnrNode = root.path("pooled_metrics").path("psnr").path("mean");
            JsonNode ssimNode = root.path("pooled_metrics").path("float_ssim").path("mean");
            double vmaf = meanNode.isNumber() ? meanNode.asDouble() : 0.0;
            Double psnr = psnrNode.isNumber() ? psnrNode.asDouble() : null;
            Double ssim = ssimNode.isNumber() ? ssimNode.asDouble() : null;
            String artifactUri = uploadVmafArtifact(uploadId, renditionSuffix, logPath);
            return new QualityReport(
                    renditionSuffix,
                    "VMAF",
                    vmaf,
                    psnr,
                    ssim,
                    true,
                    artifactUri,
                    "computed",
                    Instant.now()
            );
        } catch (Exception e) {
            return new QualityReport(
                    renditionSuffix,
                    "VMAF",
                    0.0,
                    null,
                    null,
                    false,
                    null,
                    "VQS error: " + e.getMessage(),
                    Instant.now()
            );
        } finally {
            if (logPath != null) {
                try {
                    Files.deleteIfExists(logPath);
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    private String readAll(InputStream inputStream) throws Exception {
        try (inputStream; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            inputStream.transferTo(baos);
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private String uploadVmafArtifact(String uploadId, String renditionSuffix, Path logPath) {
        try {
            byte[] payload = Files.readAllBytes(logPath);
            String key = "analysis/" + uploadId + "/vqs/" + renditionSuffix + ".vmaf.json";
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
