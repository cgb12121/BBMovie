package bbmovie.transcode.cas.processing;

import bbmovie.transcode.cas.analysis.CasLadderGenerationService;
import bbmovie.transcode.cas.analysis.ComplexityAnalysisService;
import bbmovie.transcode.cas.analysis.ComplexityAnalysisV2Service;
import bbmovie.transcode.cas.analysis.ComplexityProfile;
import bbmovie.transcode.cas.analysis.SourceVideoMetadata;
import bbmovie.transcode.cas.config.CasMediaProcessingProperties;
import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.DecisionHintsV2;
import bbmovie.transcode.contracts.dto.EncodeBitrateStrategy;
import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import bbmovie.transcode.contracts.dto.SubInfo;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.temporal.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;
import org.apache.commons.lang3.math.Fraction;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

@Slf4j
public class CasMinioProbeManifestService implements CasProcessingService {

    /** Single HLS subtitle group for all tracks so variants can reference SUBTITLES="subs". */
    private static final String HLS_SUBTITLES_GROUP_ID = "subs";

    private final MinioClient minioClient;
    private final FFprobe ffprobe;
    private final CasMediaProcessingProperties properties;
    private final ComplexityAnalysisService complexityAnalysisService;
    private final ComplexityAnalysisV2Service complexityAnalysisV2Service;
    private final CasLadderGenerationService ladderGenerationService;
    private final CasProfileCompatibilityAdapter compatibilityAdapter;

    public CasMinioProbeManifestService(
            MinioClient minioClient,
            FFprobe ffprobe,
            CasMediaProcessingProperties properties,
            ComplexityAnalysisService complexityAnalysisService,
            ComplexityAnalysisV2Service complexityAnalysisV2Service,
            CasLadderGenerationService ladderGenerationService,
            CasProfileCompatibilityAdapter compatibilityAdapter) {
        this.minioClient = minioClient;
        this.ffprobe = ffprobe;
        this.properties = properties;
        this.complexityAnalysisService = complexityAnalysisService;
        this.complexityAnalysisV2Service = complexityAnalysisV2Service;
        this.ladderGenerationService = ladderGenerationService;
        this.compatibilityAdapter = compatibilityAdapter;
    }

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

            SourceVideoMetadata sourceMeta = new SourceVideoMetadata(video.width, video.height, duration, video.codec_name);
            SourceProfileV2 sourceProfileV2 = buildSourceProfile(uploadId, bucket, key, probe, video, duration);
            ComplexityProfileV2 profileV2 = analyzeProfileV2(sourceProfileV2, sourceMeta);
            ComplexityProfile profile = compatibilityAdapter.toLegacyComplexityProfile(profileV2);
            DecisionHintsV2 hints = profileV2.decisionHints();
            log.info("[cas] profile-v2 uploadId={} riskClass={} score={} policyVersion={} topFactors={}",
                    uploadId, profileV2.riskClass(), profileV2.complexityScore(), profileV2.policyVersion(), profileV2.topFactors());

            var ladder = ladderGenerationService.generateAdaptiveEncodingLadder(sourceMeta, profile.recipeHints(), hints);
            List<String> suffixes = ladderGenerationService.toSuffixes(ladder);
            int peakCost = ladderGenerationService.calculatePeakCost(suffixes);
            int totalCost = ladderGenerationService.calculateTotalCost(suffixes);
            log.info("[cas] adaptive ladder uploadId={} rungs={} peakCost={} totalCost={} fallbackReason={}",
                    uploadId, suffixes, peakCost, totalCost, profileV2.fallbackReason());
            log.debug("[cas] adaptive policy uploadId={} hints={}", uploadId, hints);

            heartbeatCasDetails(profile, suffixes);
            heartbeatV2Details(profileV2);

            return compatibilityAdapter.toMetadataDto(sourceProfileV2, profileV2);
        } catch (Exception e) {
            throw new RuntimeException("analyzeSource failed for " + bucket + "/" + key, e);
        } finally {
            deleteDir(workDir);
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
            try (var w = Files.newBufferedWriter(masterFile, StandardCharsets.UTF_8)) {
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
            deleteDir(workDir);
        }
    }

    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        String masterKey = properties.getMoviesKeyPrefix() + "/" + uploadId + "/master.m3u8";
        Path workDir = null;
        try {
            if (subs == null || subs.isEmpty()) {
                return new ManifestUpdateDTO(masterKey, false);
            }
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "subs-" + uploadId + "-");
            Path localMaster = workDir.resolve("master.m3u8");
            download(properties.getHlsBucket(), masterKey, localMaster);

            String content = Files.readString(localMaster, StandardCharsets.UTF_8);
            String withVariantRefs = appendSubtitleGroupToStreamInfLines(content, HLS_SUBTITLES_GROUP_ID);
            StringBuilder out = new StringBuilder(withVariantRefs);
            if (!withVariantRefs.endsWith("\n")) {
                out.append('\n');
            }
            int i = 1;
            boolean firstTrack = true;
            for (SubInfo sub : subs) {
                String uri = relativeUriFromMaster(masterKey, sub.objectKey());
                if (content.contains("URI=\"" + uri + "\"")) {
                    log.debug("[cas] integrateSubtitles skip duplicate URI={}", uri);
                    i++;
                    continue;
                }
                String lang = sub.language() != null ? sub.language() : "und";
                String name = sub.language() != null ? sub.language() : ("track" + i);
                out.append("#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"").append(HLS_SUBTITLES_GROUP_ID)
                        .append("\",NAME=\"").append(escapeM3u8Attr(name))
                        .append("\",DEFAULT=").append(firstTrack ? "YES" : "NO")
                        .append(",AUTOSELECT=YES,FORCED=NO,LANGUAGE=\"").append(escapeM3u8Attr(lang))
                        .append("\",URI=\"").append(escapeM3u8Attr(uri)).append("\"\n");
                firstTrack = false;
                i++;
            }
            Files.writeString(localMaster, out.toString(), StandardCharsets.UTF_8);
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(properties.getHlsBucket())
                            .object(masterKey)
                            .filename(localMaster.toString())
                            .contentType("application/vnd.apple.mpegurl")
                            .build()
            );
            return new ManifestUpdateDTO(masterKey, true);
        } catch (Exception e) {
            throw new RuntimeException("integrateSubtitles failed for " + masterKey, e);
        } finally {
            deleteDir(workDir);
        }
    }

    private void download(String bucket, String key, Path targetPath) throws Exception {
        try (var response = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            heartbeatWhileCopy(response, targetPath);
        }
    }

    private void heartbeatWhileCopy(InputStream in, Path targetPath) throws IOException {
        byte[] buf = new byte[64 * 1024];
        long total = 0;
        long sinceHeartbeat = 0;
        try (OutputStream out = Files.newOutputStream(targetPath)) {
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
                total += n;
                sinceHeartbeat += n;
                if (sinceHeartbeat >= 512 * 1024) {
                    sinceHeartbeat = 0;
                    try {
                        Activity.getExecutionContext().heartbeat(total);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void heartbeatCasDetails(ComplexityProfile profile, List<String> suffixes) {
        try {
            StringJoiner sj = new StringJoiner(",");
            sj.add("contentClass=" + profile.contentClass());
            sj.add("complexityScore=" + profile.complexityScore());
            sj.add("ladder=" + String.join("|", suffixes));
            Activity.getExecutionContext().heartbeat(sj.toString());
        } catch (Exception ignored) {
        }
    }

    private void heartbeatV2Details(ComplexityProfileV2 profileV2) {
        try {
            String payload = "riskClass=" + profileV2.riskClass()
                    + ",confidence=" + profileV2.confidence()
                    + ",policyVersion=" + profileV2.policyVersion();
            Activity.getExecutionContext().heartbeat(payload);
        } catch (Exception ignored) {
        }
    }

    private ComplexityProfileV2 analyzeProfileV2(SourceProfileV2 sourceProfileV2, SourceVideoMetadata sourceMeta) {
        if (!properties.isProfileV2Enabled()) {
            return conservativeFallback(sourceProfileV2, "profile_v2_disabled", sourceMeta);
        }
        try {
            return complexityAnalysisV2Service.analyze(sourceProfileV2);
        } catch (Exception e) {
            log.warn("[cas] profile-v2 failed uploadId={} reason={}", sourceProfileV2.uploadId(), e.getMessage());
            return conservativeFallback(sourceProfileV2, "analysis_failure", sourceMeta);
        }
    }

    private ComplexityProfileV2 conservativeFallback(SourceProfileV2 sourceProfileV2, String reason, SourceVideoMetadata sourceMeta) {
        ComplexityProfile legacy = complexityAnalysisService.analyze(sourceProfileV2.uploadId(), sourceMeta);
        return new ComplexityProfileV2(
                sourceProfileV2.uploadId(),
                legacy.complexityScore(),
                legacy.contentClass().toUpperCase(),
                legacy.featureScores(),
                new DecisionHintsV2(
                        "veryfast",
                        legacy.contentClass().toUpperCase(),
                        5,
                        900,
                        6500,
                        true,
                        legacy.recipeHints().skipSuffixes().stream().toList(),
                        legacy.featureScores(),
                        List.of("fallback=" + reason),
                        properties.getProfileV2AnalysisVersion(),
                        properties.getProfileV2PolicyVersion(),
                        EncodeBitrateStrategy.VBV_CRF_CAP,
                        Integer.valueOf(22)
                ),
                List.of("legacy-fallback"),
                properties.getProfileV2AnalysisVersion(),
                properties.getProfileV2PolicyVersion(),
                sourceProfileV2.confidence(),
                reason,
                legacy.analyzedAt()
        );
    }

    private SourceProfileV2 buildSourceProfile(
            String uploadId,
            String bucket,
            String key,
            FFmpegProbeResult probe,
            FFmpegStream video,
            double duration) {
        FFmpegStream audio = probe.getStreams().stream()
                .filter(s -> s.codec_type == CodecType.AUDIO)
                .findFirst()
                .orElse(null);
        return new SourceProfileV2(
                uploadId,
                bucket,
                key,
                video.width,
                video.height,
                duration,
                video.codec_name,
                probe.getFormat() != null ? probe.getFormat().format_name : "unknown",
                parseFps(video.r_frame_rate),
                fpsMode(video.avg_frame_rate, video.r_frame_rate),
                audio != null ? Math.max(0, audio.channels) : 0,
                Math.max(0, video.bits_per_raw_sample),
                video.pix_fmt != null ? video.pix_fmt : "unknown",
                "cas-local",
                0.9,
                properties.getProfileV2AnalysisVersion(),
                fingerprint(uploadId, bucket, key, video.width, video.height, video.codec_name),
                List.of()
        );
    }

    private static double parseFps(Fraction ratio) {
        if (ratio == null) {
            return 0.0;
        }
        try {
            return ratio.doubleValue();
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static String fpsMode(Fraction avg, Fraction raw) {
        double avgValue = parseFps(avg);
        double rawValue = parseFps(raw);
        if (avgValue <= 0 || rawValue <= 0) {
            return "unknown";
        }
        return Math.abs(avgValue - rawValue) < 0.05 ? "cfr" : "vfr";
    }

    private static String fingerprint(String uploadId, String bucket, String key, int width, int height, String codec) {
        try {
            String raw = uploadId + "|" + bucket + "|" + key + "|" + width + "|" + height + "|" + codec;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (Exception ignored) {
            return Integer.toHexString(rawHash(uploadId, bucket, key));
        }
    }

    private static int rawHash(String uploadId, String bucket, String key) {
        return (uploadId + "|" + bucket + "|" + key).hashCode();
    }

    private void deleteDir(Path workDir) {
        if (workDir == null) {
            return;
        }
        try {
            FileSystemUtils.deleteRecursively(workDir);
        } catch (IOException ignored) {
        }
    }

    private static String escapeM3u8Attr(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Adds SUBTITLES group reference on variant lines so players bind one group to all renditions. */
    private static String appendSubtitleGroupToStreamInfLines(String content, String groupId) {
        String[] lines = content.split("\\R", -1);
        StringBuilder sb = new StringBuilder(content.length() + 32);
        for (int li = 0; li < lines.length; li++) {
            if (li > 0) {
                sb.append('\n');
            }
            String line = lines[li];
            if (line.startsWith("#EXT-X-STREAM-INF:") && !line.contains("SUBTITLES=")) {
                sb.append(line).append(",SUBTITLES=\"").append(groupId).append("\"");
            } else {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String relativeUriFromMaster(String masterObjectKey, String subtitleObjectKey) {
        int lastSlash = masterObjectKey.lastIndexOf('/');
        String masterDir = lastSlash >= 0 ? masterObjectKey.substring(0, lastSlash + 1) : "";
        if (subtitleObjectKey != null && subtitleObjectKey.startsWith(masterDir)) {
            return subtitleObjectKey.substring(masterDir.length());
        }
        return subtitleObjectKey != null ? subtitleObjectKey : "";
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
            default -> 0;
        };
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
