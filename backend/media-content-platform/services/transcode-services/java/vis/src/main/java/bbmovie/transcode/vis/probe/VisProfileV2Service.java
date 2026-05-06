package bbmovie.transcode.vis.probe;

import bbmovie.transcode.contracts.dto.SourceProfileV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;
import org.apache.commons.lang3.math.Fraction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Builds VIS {@link SourceProfileV2} by combining fast-probe and optional deep-probe paths.
 *
 * <p>Fast probe always runs first; deep probe is conditionally triggered by
 * {@link VisProbeDecisionPolicy} when confidence gates are not met.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisProfileV2Service {

    private final VisFastProbeService visFastProbeService;
    private final VisMetadataService visMetadataService;
    private final VisPresignedUrlService visPresignedUrlService;
    private final VisProbeDecisionPolicy visProbeDecisionPolicy;

    @Value("${vis.profile-v2.analysis-version:v2.0}")
    private String analysisVersion;

    /** Produces normalized source profile with decision-gated deep-probe fallback behavior. */
    public SourceProfileV2 analyze(String uploadId, String bucket, String key) {
        VisProbeOutcome fast = visFastProbeService.probe(bucket, key);
        VisProbeDecisionPolicy.ProbeDecision decision = visProbeDecisionPolicy.decide(fast, key);
        if (!decision.deepProbeRequired()) {
            return fromFast(uploadId, bucket, key, fast, decision.confidence(), decision.gateReasons());
        }
        try {
            String url = visPresignedUrlService.generateProbeUrl(bucket, key);
            FFmpegProbeResult deepResult = visMetadataService.probeResultFromUrl(url);
            SourceProfileV2 deepProfile = fromProbeResult(uploadId, bucket, key, deepResult, decision.gateReasons());
            log.debug("[vis] deep probe selected uploadId={} reason={}", uploadId, decision.gateReasons());
            return deepProfile;
        } catch (Exception e) {
            log.warn("[vis] deep probe failed for uploadId={} {}/{}; falling back to fast profile: {}",
                    uploadId, bucket, key, e.getMessage());
            return fromFast(uploadId, bucket, key, fast, 0.45, List.of("deep_probe_failed"));
        }
    }

    /** Maps fast-probe outcome into conservative/default v2 source profile fields. */
    private SourceProfileV2 fromFast(
            String uploadId,
            String bucket,
            String key,
            VisProbeOutcome fast,
            double confidence,
            List<String> gateReasons) {
        return new SourceProfileV2(
                uploadId,
                bucket,
                key,
                fast.width(),
                fast.height(),
                fast.duration(),
                fast.codec(),
                guessContainer(key),
                0.0,
                "unknown",
                0,
                0,
                "unknown",
                "fast",
                clamp(confidence),
                analysisVersion,
                fingerprint(uploadId, bucket, key, fast.width(), fast.height(), fast.codec()),
                gateReasons
        );
    }

    /** Maps full ffprobe result into rich v2 source profile fields. */
    private SourceProfileV2 fromProbeResult(
            String uploadId,
            String bucket,
            String key,
            FFmpegProbeResult probeResult,
            List<String> gateReasons) {
        FFmpegStream video = probeResult.getStreams().stream()
                .filter(s -> s.codec_type == CodecType.VIDEO)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No video stream found"));
        FFmpegStream audio = probeResult.getStreams().stream()
                .filter(s -> s.codec_type == CodecType.AUDIO)
                .findFirst()
                .orElse(null);
        // Prefer container duration when available; fallback to stream duration otherwise.
        double duration = probeResult.getFormat() != null && probeResult.getFormat().duration > 0
                ? probeResult.getFormat().duration
                : (video.duration > 0 ? video.duration : 0.0);
        return new SourceProfileV2(
                uploadId,
                bucket,
                key,
                video.width,
                video.height,
                duration,
                video.codec_name,
                probeResult.getFormat() != null ? probeResult.getFormat().format_name : guessContainer(key),
                parseFraction(video.r_frame_rate),
                fpsMode(video.avg_frame_rate, video.r_frame_rate),
                audio != null && audio.channels > 0 ? audio.channels : 0,
                video.bits_per_raw_sample,
                video.pix_fmt,
                "deep",
                0.97,
                analysisVersion,
                fingerprint(uploadId, bucket, key, video.width, video.height, video.codec_name),
                gateReasons
        );
    }

    /** Detects CFR/VFR heuristic from avg/raw frame-rate values. */
    private static String fpsMode(Fraction avg, Fraction raw) {
        double avgValue = parseFraction(avg);
        double rawValue = parseFraction(raw);
        if (avgValue <= 0 || rawValue <= 0) {
            return "unknown";
        }
        return Math.abs(avgValue - rawValue) < 0.05 ? "cfr" : "vfr";
    }

    /** Parses Apache Fraction safely into double fps value. */
    private static double parseFraction(Fraction value) {
        if (value == null) {
            return 0.0;
        }
        try {
            return value.doubleValue();
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    /** Best-effort container guess from object key extension. */
    private static String guessContainer(String key) {
        if (key == null) {
            return "unknown";
        }
        int dot = key.lastIndexOf('.');
        if (dot < 0 || dot + 1 >= key.length()) {
            return "unknown";
        }
        return key.substring(dot + 1).toLowerCase();
    }

    /** Builds short stable fingerprint for profile dedupe/correlation. */
    private static String fingerprint(String uploadId, String bucket, String key, int width, int height, String codec) {
        try {
            String raw = uploadId + "|" + bucket + "|" + key + "|" + width + "|" + height + "|" + codec;
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 12);
        } catch (Exception ignored) {
            return Integer.toHexString((uploadId + bucket + key).hashCode());
        }
    }

    /** Clamps confidence into [0,1] interval. */
    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
