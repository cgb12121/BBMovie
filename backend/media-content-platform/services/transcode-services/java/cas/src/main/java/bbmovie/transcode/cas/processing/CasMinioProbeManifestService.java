package bbmovie.transcode.cas.processing;

import bbmovie.transcode.cas.analysis.CasLadderGenerationService;
import bbmovie.transcode.cas.analysis.ComplexityAnalysisService;
import bbmovie.transcode.cas.analysis.ComplexityProfile;
import bbmovie.transcode.cas.analysis.SourceVideoMetadata;
import bbmovie.transcode.cas.config.CasMediaProcessingProperties;
import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SubInfo;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.temporal.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

@Slf4j
public class CasMinioProbeManifestService implements CasProcessingService {

    private final MinioClient minioClient;
    private final FFprobe ffprobe;
    private final CasMediaProcessingProperties properties;
    private final ComplexityAnalysisService complexityAnalysisService;
    private final CasLadderGenerationService ladderGenerationService;

    public CasMinioProbeManifestService(
            MinioClient minioClient,
            FFprobe ffprobe,
            CasMediaProcessingProperties properties,
            ComplexityAnalysisService complexityAnalysisService,
            CasLadderGenerationService ladderGenerationService) {
        this.minioClient = minioClient;
        this.ffprobe = ffprobe;
        this.properties = properties;
        this.complexityAnalysisService = complexityAnalysisService;
        this.ladderGenerationService = ladderGenerationService;
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
            ComplexityProfile profile = complexityAnalysisService.analyze(uploadId, sourceMeta);
            log.info("[cas] legacy CAS uploadId={} contentClass={} score={} recipeSkip={}",
                    uploadId, profile.contentClass(), profile.complexityScore(), profile.recipeHints().skipSuffixes());

            var ladder = ladderGenerationService.generateEncodingLadder(sourceMeta, profile.recipeHints());
            List<String> suffixes = ladderGenerationService.toSuffixes(ladder);
            int peakCost = ladderGenerationService.calculatePeakCost(suffixes);
            int totalCost = ladderGenerationService.calculateTotalCost(suffixes);
            log.info("[cas] legacy ladder uploadId={} rungs={} peakCost={} totalCost={}",
                    uploadId, suffixes, peakCost, totalCost);

            heartbeatCasDetails(profile, suffixes);

            return new MetadataDTO(video.width, video.height, duration, video.codec_name);
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
            StringBuilder out = new StringBuilder(content);
            if (!content.endsWith("\n")) {
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
                String groupId = "subs" + i;
                out.append("#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"").append(groupId)
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
                .filter(s -> s.codec_type == FFmpegStream.CodecType.VIDEO)
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
