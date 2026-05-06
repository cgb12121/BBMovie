package bbmovie.transcode.vvs.activity;

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
import bbmovie.transcode.vvs.processing.VvsQualityProcessingService;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Temporal activity adapter for VVS quality queue.
 *
 * <p>This worker is intentionally scoped to validation only; non-quality activity calls are
 * rejected so workflow routing bugs fail fast.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VvsMediaActivities implements MediaActivities {

    private final VvsQualityProcessingService vvsQualityProcessingService;

    @Override
    /** Not served on quality queue; analyze belongs to analyzer services. */
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnQualityQueue("analyzeSource"));
    }

    @Override
    /** Not served on quality queue; encode belongs to encoder services. */
    public RungResultDTO encodeResolution(EncodeRequest request) {
        throw Activity.wrap(notOnQualityQueue("encodeResolution"));
    }

    @Override
    /** Validates one encoded rendition and returns pass/fail score output. */
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        log.debug("[vvs] validateAndScore {}", request.renditionLabel());
        return vvsQualityProcessingService.validateAndScore(request);
    }

    @Override
    /** Not served on quality queue; manifest generation belongs to analyzer/orchestrator path. */
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        throw Activity.wrap(notOnQualityQueue("generateMasterManifest"));
    }

    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnQualityQueue("normalizeSubtitle"));
    }

    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        throw Activity.wrap(notOnQualityQueue("translateSubtitle"));
    }

    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        throw Activity.wrap(notOnQualityQueue("integrateSubtitles"));
    }

    /** Shared error helper for unsupported methods on this queue-specific worker. */
    private static IllegalStateException notOnQualityQueue(String method) {
        return new IllegalStateException("VVS worker only serves quality-queue; unexpected call: " + method);
    }
}
