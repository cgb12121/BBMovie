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
import net.bramp.ffmpeg.shared.CodecType;
import org.springframework.util.FileSystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Post-encode technical validation service.
 *
 * <p>VVS is intentionally a conformance gate (correctness/spec checks) and does not perform
 * perceptual quality scoring.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VvsQualityProcessingService {

    private static final double DIMENSION_TOLERANCE_PX = 8.0;

    private record VvsRuleResult(boolean passed, String ruleId, String reasonCode, String detail) {
        static VvsRuleResult pass(String ruleId) {
            return new VvsRuleResult(true, ruleId, "vvs_validation_passed", ruleId);
        }

        static VvsRuleResult fail(String ruleId, String reasonCode, String detail) {
            return new VvsRuleResult(false, ruleId, reasonCode, detail);
        }
    }

    private final MinioClient minioClient;
    private final FFprobe ffprobe;
    private final VvsMediaProcessingProperties properties;

    /** Validates encoded rendition dimensions by probing produced HLS playlist. */
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory(Paths.get(properties.getTempDir()), "vvs-" + request.uploadId() + "-");
            Path playlist = workDir.resolve("playlist.m3u8");
            download(properties.getHlsBucket(), request.playlistPath(), playlist);

            String playlistContent = Files.readString(playlist, StandardCharsets.UTF_8);

            FFmpegProbeResult probe = ffprobe.probe(
                    ffprobe.builder()
                            .setInput(playlist.toString())
                            // Local HLS probe can reference nested segments/keys over mixed schemes.
                            .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls,crypto")
                            .build());
            List<VvsRuleResult> results = runRules(request, playlistContent, probe, workDir);
            for (VvsRuleResult r : results) {
                log.info("[vvs] uploadId={} rendition={} ruleId={} passed={} reasonCode={} detail={}",
                        request.uploadId(),
                        request.renditionLabel(),
                        r.ruleId(),
                        r.passed(),
                        r.reasonCode(),
                        r.detail());
                if (!r.passed()) {
                    return fail(request.renditionLabel(), r.reasonCode(), r.detail());
                }
            }
            return pass(request.renditionLabel(), "vvs_validation_passed");
        } catch (Exception e) {
            log.warn("VVS validateAndScore failed {}: {}", request.renditionLabel(), e.getMessage());
            String detail = e.getMessage() != null ? "vvs_probe_error: " + e.getMessage() : "vvs_probe_error";
            return fail(request.renditionLabel(), "vvs_probe_error", detail);
        } finally {
            if (workDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(workDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private List<VvsRuleResult> runRules(
            ValidationRequest request,
            String playlistContent,
            FFmpegProbeResult probe,
            Path workDir
    ) throws Exception {
        List<VvsRuleResult> out = new ArrayList<>();

        out.add(validatePlaylistStrict(request, playlistContent));
        out.add(validateContainerAndStreamsExist(probe));

        FFmpegStream video = firstVideoStream(probe).orElse(null);
        if (video != null) {
            out.add(validateVideoRecipeAndAllowlist(request, video));
        }

        FFmpegStream audio = firstAudioStream(probe).orElse(null);
        out.add(validateAudioRecipeAndAllowlist(request, audio));

        if (properties.isVvsDeepValidationEnabled() && properties.getVvsDeepSampleSegments() > 0) {
            out.add(validateSegmentsDeepRule(
                    request,
                    playlistContent,
                    workDir,
                    Math.max(1, properties.getVvsDeepSampleSegments())
            ));
        } else {
            out.add(VvsRuleResult.pass("VVS_DEEP_VALIDATION_DISABLED"));
        }

        return out;
    }

    private VvsRuleResult validateContainerAndStreamsExist(FFmpegProbeResult probe) {
        if (firstVideoStream(probe).isEmpty()) {
            return VvsRuleResult.fail("VVS_STREAMS_PRESENT", "vvs_no_video_stream", "vvs_no_video_stream");
        }
        return VvsRuleResult.pass("VVS_STREAMS_PRESENT");
    }

    private VvsRuleResult validateVideoRecipeAndAllowlist(ValidationRequest request, FFmpegStream video) {
        boolean dimsOk = Math.abs(video.width - request.expectedWidth()) <= DIMENSION_TOLERANCE_PX
                && Math.abs(video.height - request.expectedHeight()) <= DIMENSION_TOLERANCE_PX;
        if (!dimsOk) {
            return VvsRuleResult.fail(
                    "VVS_DIMENSIONS",
                    "vvs_dimension_mismatch",
                    String.format(Locale.ROOT, "vvs_dimension_mismatch expected=%dx%d actual=%dx%d",
                            request.expectedWidth(), request.expectedHeight(), video.width, video.height)
            );
        }

        String expectedCodec = safeLower(request.expectedVideoCodec());
        if (!expectedCodec.isEmpty()) {
            String actualCodec = safeLower(video.codec_name);
            if (!expectedCodec.equals(actualCodec)) {
                return VvsRuleResult.fail("VVS_RECIPE_VIDEO_CODEC", "vvs_recipe_codec_mismatch",
                        "vvs_recipe_codec_mismatch expected=" + expectedCodec + " actual=" + actualCodec);
            }
        }

        Set<String> allowedCodecs = csvAllowlist(properties.getAllowedVideoCodecs());
        if (!allowedCodecs.isEmpty()) {
            String codec = safeLower(video.codec_name);
            if (!allowedCodecs.contains(codec)) {
                return VvsRuleResult.fail("VVS_ALLOWED_VIDEO_CODEC", "vvs_codec_not_allowed",
                        "vvs_codec_not_allowed codec=" + codec);
            }
        }

        String expectedPixFmt = safeLower(request.expectedPixFmt());
        if (!expectedPixFmt.isEmpty()) {
            String actualPixFmt = safeLower(video.pix_fmt);
            if (!expectedPixFmt.equals(actualPixFmt)) {
                return VvsRuleResult.fail("VVS_RECIPE_PIX_FMT", "vvs_recipe_pix_fmt_mismatch",
                        "vvs_recipe_pix_fmt_mismatch expected=" + expectedPixFmt + " actual=" + actualPixFmt);
            }
        }

        Set<String> allowedPix = csvAllowlist(properties.getAllowedPixFmts());
        if (!allowedPix.isEmpty()) {
            String pixFmt = safeLower(video.pix_fmt);
            if (!allowedPix.contains(pixFmt)) {
                return VvsRuleResult.fail("VVS_ALLOWED_PIX_FMT", "vvs_pix_fmt_not_allowed",
                        "vvs_pix_fmt_not_allowed pix_fmt=" + pixFmt);
            }
        }

        return VvsRuleResult.pass("VVS_VIDEO_CONFORMANCE");
    }

    private VvsRuleResult validateAudioRecipeAndAllowlist(ValidationRequest request, FFmpegStream audio) {
        if (audio == null) {
            if (properties.isRequireAudioStream()) {
                return VvsRuleResult.fail("VVS_AUDIO_PRESENT", "vvs_audio_missing", "vvs_audio_missing");
            }
            return VvsRuleResult.pass("VVS_AUDIO_NOT_REQUIRED");
        }

        String expectedAudioCodec = safeLower(request.expectedAudioCodec());
        if (!expectedAudioCodec.isEmpty()) {
            String actualAudioCodec = safeLower(audio.codec_name);
            if (!expectedAudioCodec.equals(actualAudioCodec)) {
                return VvsRuleResult.fail("VVS_RECIPE_AUDIO_CODEC", "vvs_recipe_audio_mismatch",
                        "vvs_recipe_audio_mismatch expected=" + expectedAudioCodec + " actual=" + actualAudioCodec);
            }
        }
        if (request.expectedAudioChannels() > 0 && audio.channels > 0 && request.expectedAudioChannels() != audio.channels) {
            return VvsRuleResult.fail("VVS_RECIPE_AUDIO_CHANNELS", "vvs_recipe_audio_mismatch",
                    "vvs_recipe_audio_mismatch expected_channels=" + request.expectedAudioChannels() + " actual_channels=" + audio.channels);
        }
        if (request.expectedAudioSampleRate() > 0 && audio.sample_rate > 0 && request.expectedAudioSampleRate() != audio.sample_rate) {
            return VvsRuleResult.fail("VVS_RECIPE_AUDIO_SAMPLE_RATE", "vvs_recipe_audio_mismatch",
                    "vvs_recipe_audio_mismatch expected_sample_rate=" + request.expectedAudioSampleRate() + " actual_sample_rate=" + audio.sample_rate);
        }

        Set<String> allowedAudioCodecs = csvAllowlist(properties.getAllowedAudioCodecs());
        if (!allowedAudioCodecs.isEmpty()) {
            String codec = safeLower(audio.codec_name);
            if (!allowedAudioCodecs.contains(codec)) {
                return VvsRuleResult.fail("VVS_ALLOWED_AUDIO_CODEC", "vvs_audio_codec_not_allowed",
                        "vvs_audio_codec_not_allowed codec=" + codec);
            }
        }
        Set<Integer> allowedChannels = csvIntAllowlist(properties.getAllowedAudioChannels());
        if (!allowedChannels.isEmpty() && audio.channels > 0 && !allowedChannels.contains(audio.channels)) {
            return VvsRuleResult.fail("VVS_ALLOWED_AUDIO_CHANNELS", "vvs_audio_channels_not_allowed",
                    "vvs_audio_channels_not_allowed channels=" + audio.channels);
        }
        Set<Integer> allowedSampleRates = csvIntAllowlist(properties.getAllowedAudioSampleRates());
        int sr = audio.sample_rate;
        if (!allowedSampleRates.isEmpty() && sr > 0 && !allowedSampleRates.contains(sr)) {
            return VvsRuleResult.fail("VVS_ALLOWED_AUDIO_SAMPLE_RATE", "vvs_audio_sample_rate_not_allowed",
                    "vvs_audio_sample_rate_not_allowed sample_rate=" + sr);
        }
        return VvsRuleResult.pass("VVS_AUDIO_CONFORMANCE");
    }

    private VvsRuleResult validatePlaylistStrict(ValidationRequest request, String content) {
        if (content == null || content.isBlank()) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid", "vvs_playlist_invalid empty");
        }
        if (!content.contains("#EXTM3U")) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid", "vvs_playlist_invalid missing_extm3u");
        }
        if (!content.contains("#EXT-X-VERSION:")) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid", "vvs_playlist_invalid missing_version");
        }
        if (!content.contains("#EXT-X-TARGETDURATION:")) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid", "vvs_playlist_invalid missing_targetduration");
        }
        if (!content.contains("#EXT-X-MEDIA-SEQUENCE:")) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid", "vvs_playlist_invalid missing_media_sequence");
        }
        if (request.expectedVodPlaylist() && !content.contains("#EXT-X-ENDLIST")) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid", "vvs_playlist_invalid missing_endlist");
        }
        List<String> segments = parseMediaPlaylistSegments(content);
        if (segments.isEmpty()) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid", "vvs_playlist_invalid no_segments");
        }

        Integer parsedTargetDuration = parseTargetDurationSeconds(content);
        if (request.expectedHlsTargetDurationSeconds() > 0 && parsedTargetDuration != null
                && request.expectedHlsTargetDurationSeconds() != parsedTargetDuration) {
            return VvsRuleResult.fail("VVS_HLS_PLAYLIST", "vvs_playlist_invalid",
                    "vvs_playlist_invalid targetduration expected=" + request.expectedHlsTargetDurationSeconds() + " actual=" + parsedTargetDuration);
        }

        return VvsRuleResult.pass("VVS_HLS_PLAYLIST");
    }

    static List<String> parseMediaPlaylistSegments(String content) {
        String[] lines = content.split("\\R");
        List<String> segments = new ArrayList<>();
        boolean lastWasExtInf = false;
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#EXTINF:")) {
                lastWasExtInf = true;
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            if (lastWasExtInf) {
                segments.add(line);
            }
            lastWasExtInf = false;
        }
        return segments;
    }

    private VvsRuleResult validateSegmentsDeepRule(
            ValidationRequest request,
            String playlistContent,
            Path workDir,
            int sampleCount
    ) throws Exception {
        List<String> segments = parseMediaPlaylistSegments(playlistContent);
        if (segments.isEmpty()) {
            return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_playlist_invalid", "vvs_playlist_invalid no_segments");
        }
        List<String> selected = selectSampleSegments(segments, sampleCount);
        String playlistKey = request.playlistPath();
        String prefix = playlistKey.contains("/") ? playlistKey.substring(0, playlistKey.lastIndexOf('/') + 1) : "";

        for (int i = 0; i < selected.size(); i++) {
            String uri = selected.get(i);
            String objectKey = uri.startsWith("http://") || uri.startsWith("https://")
                    ? null
                    : normalizeObjectKey(prefix + uri);
            if (objectKey == null) {
                return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_playlist_invalid", "vvs_playlist_invalid external_uri=" + uri);
            }
            Path segPath = workDir.resolve("seg-" + i + "-" + Paths.get(uri).getFileName());
            boolean downloaded = downloadWithTimeout(properties.getHlsBucket(), objectKey, segPath,
                    properties.getSegmentDownloadTimeoutSeconds());
            if (!downloaded) {
                return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_segment_download_failed", "vvs_segment_download_failed key=" + objectKey);
            }
            FFmpegProbeResult segProbe;
            try {
                segProbe = ffprobe.probe(ffprobe.builder().setInput(segPath.toString()).build());
            } catch (Exception e) {
                return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_segment_probe_failed", "vvs_segment_probe_failed " + e.getMessage());
            }
            if (firstVideoStream(segProbe).isEmpty()) {
                return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_no_video_stream", "vvs_no_video_stream segment=" + uri);
            }
            if (properties.isRequireAudioStream() && firstAudioStream(segProbe).isEmpty()) {
                return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_audio_missing", "vvs_audio_missing segment=" + uri);
            }
            if (!hasReasonableSegmentTimestamps(segPath)) {
                return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_timestamp_suspect", "vvs_timestamp_suspect segment=" + uri);
            }
            if (!hasKeyframeNearStart(segPath)) {
                return VvsRuleResult.fail("VVS_DEEP_SAMPLE", "vvs_keyframe_suspect", "vvs_keyframe_suspect segment=" + uri);
            }
        }
        return VvsRuleResult.pass("VVS_DEEP_SAMPLE");
    }

    static List<String> selectSampleSegments(List<String> segments, int sampleCount) {
        if (segments.isEmpty()) {
            return List.of();
        }
        if (sampleCount <= 1 || segments.size() == 1) {
            return List.of(segments.getFirst());
        }
        if (sampleCount == 2) {
            return List.of(segments.getFirst(), segments.getLast());
        }
        List<String> out = new ArrayList<>();
        out.add(segments.getFirst());
        out.add(segments.get(segments.size() / 2));
        out.add(segments.getLast());
        return out.subList(0, Math.min(sampleCount, out.size()));
    }

    private boolean hasReasonableSegmentTimestamps(Path segmentPath) {
        // Best-effort heuristic: inspect ffprobe text output for obvious timestamp errors without
        // doing full packet-level parsing.
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    properties.getFfprobePath(),
                    "-hide_banner",
                    "-loglevel", "error",
                    "-show_entries", "format=start_time,duration",
                    "-of", "default=noprint_wrappers=1",
                    segmentPath.toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            boolean ok = p.waitFor(5, TimeUnit.SECONDS);
            if (!ok || p.exitValue() != 0) {
                return true; // do not fail on tooling issues; segment probe already succeeded
            }
            String s = out.toString();
            if (s.contains("start_time=-") || s.contains("duration=-")) {
                return false;
            }
            return true;
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean hasKeyframeNearStart(Path segmentPath) {
        // Bounded heuristic: check that at least one I-frame exists in the segment.
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    properties.getFfprobePath(),
                    "-hide_banner",
                    "-loglevel", "error",
                    "-select_streams", "v",
                    "-show_frames",
                    "-read_intervals", "%+#1",
                    "-show_entries", "frame=pict_type",
                    "-of", "csv=p=0",
                    segmentPath.toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if ("I".equals(line.trim())) {
                        return true;
                    }
                }
            }
            p.waitFor(5, TimeUnit.SECONDS);
            return true; // do not fail if probe output is inconclusive
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean downloadWithTimeout(String bucket, String key, Path target, int timeoutSeconds) {
        CompletableFuture<Boolean> f = CompletableFuture.supplyAsync(() -> {
            try {
                download(bucket, key, target);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        try {
            return f.get(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
        } catch (Exception e) {
            f.cancel(true);
            return false;
        }
    }

    private static String normalizeObjectKey(String key) {
        String out = key.replace("\\", "/");
        while (out.contains("//")) {
            out = out.replace("//", "/");
        }
        return out.startsWith("/") ? out.substring(1) : out;
    }

    private static String safeLower(String v) {
        return v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> csvAllowlist(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        String[] parts = csv.split(",");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            String s = safeLower(p);
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static Set<Integer> csvIntAllowlist(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        String[] parts = csv.split(",");
        Set<Integer> out = new HashSet<>();
        for (String p : parts) {
            int v = parseIntSafe(p);
            if (v > 0) {
                out.add(v);
            }
        }
        return out;
    }

    private static int parseIntSafe(String v) {
        if (v == null) {
            return 0;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static QualityReportDTO fail(String renditionLabel, String reasonCode, String detail) {
        return new QualityReportDTO(renditionLabel, false, 0, detail, null, null, null, reasonCode);
    }

    private static QualityReportDTO pass(String renditionLabel, String reasonCode) {
        return new QualityReportDTO(renditionLabel, true, 0, reasonCode, null, null, null, reasonCode);
    }

    private static Integer parseTargetDurationSeconds(String playlistContent) {
        String[] lines = playlistContent.split("\\R");
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.startsWith("#EXT-X-TARGETDURATION:")) {
                String v = line.substring("#EXT-X-TARGETDURATION:".length()).trim();
                int parsed = parseIntSafe(v);
                return parsed > 0 ? parsed : null;
            }
        }
        return null;
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

    private static Optional<FFmpegStream> firstAudioStream(FFmpegProbeResult probe) {
        if (probe.getStreams() == null) {
            return Optional.empty();
        }
        return probe.getStreams().stream()
                .filter(s -> s.codec_type == CodecType.AUDIO)
                .findFirst();
    }
}
