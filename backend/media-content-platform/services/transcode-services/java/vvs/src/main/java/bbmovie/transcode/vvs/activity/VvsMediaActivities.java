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
 * Temporal activity adapter for VVS validation queue.
 *
 * <p>This worker is intentionally scoped to validation only; non-quality activity calls are
 * rejected so workflow routing bugs fail fast.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VvsMediaActivities implements MediaActivities {

    private final VvsQualityProcessingService vvsQualityProcessingService;

    /** Not served on validation queue; analyze belongs to analyzer services. */
    @Override
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnValidationQueue("analyzeSource"));
    }

    /** Not served on validation queue; encode belongs to encoder services. */
    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        throw Activity.wrap(notOnValidationQueue("encodeResolution"));
    }

    /** Validates one encoded rendition and returns validation-only outcome details. */
    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        log.debug("[vvs] validateAndScore {}", request.renditionLabel());
        return vvsQualityProcessingService.validateAndScore(request);
    }

    /** Not served on validation queue; manifest generation belongs to analyzer/orchestrator path. */
    @Override
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        throw Activity.wrap(notOnValidationQueue("generateMasterManifest"));
    }

    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnValidationQueue("normalizeSubtitle"));
    }

    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        throw Activity.wrap(notOnValidationQueue("translateSubtitle"));
    }

    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        throw Activity.wrap(notOnValidationQueue("integrateSubtitles"));
    }

    /** Shared error helper for unsupported methods on this queue-specific worker. */
    private static IllegalStateException notOnValidationQueue(String method) {
        return new IllegalStateException("VVS worker only serves validation-queue; unexpected call: " + method);
    }
}
