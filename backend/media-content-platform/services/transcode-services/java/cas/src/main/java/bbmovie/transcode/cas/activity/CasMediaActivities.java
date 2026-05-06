package bbmovie.transcode.cas.activity;

import bbmovie.transcode.cas.processing.CasProcessingService;
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
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * Temporal activity adapter for CAS.
 *
 * <p>CAS only serves analysis and manifest assembly. Encode/validate/subtitle-transform calls are
 * intentionally rejected so the workflow does not route unsupported work to this worker queue.</p>
 */
public class CasMediaActivities implements MediaActivities {

    private final CasProcessingService casProcessingService;

    /** Probes source media and returns metadata enriched with CAS complexity decisions. */
    @Override
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        log.debug("[cas] analyzeSource uploadId={} {}/{}", uploadId, bucket, key);
        return casProcessingService.analyzeSource(uploadId, bucket, key);
    }

    /** CAS never executes encode; fail fast to surface incorrect workflow routing. */
    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        throw Activity.wrap(notOnAnalysisQueue("encodeResolution"));
    }

    /** CAS never runs quality scoring; this belongs to validation services. */
    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        throw Activity.wrap(notOnAnalysisQueue("validateAndScore"));
    }

    /** Builds and uploads a master manifest from successful rung outputs. */
    @Override
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        log.debug("[cas] generateMasterManifest rungs={}", rungs != null ? rungs.size() : 0);
        return casProcessingService.generateMasterManifest(rungs);
    }

    /** Subtitle normalization is out-of-scope for CAS queue. */
    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnAnalysisQueue("normalizeSubtitle"));
    }

    /** Subtitle translation is out-of-scope for CAS queue. */
    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        throw Activity.wrap(notOnAnalysisQueue("translateSubtitle"));
    }

    /** Injects subtitle tracks into an existing master playlist. */
    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        log.debug("[cas] integrateSubtitles uploadId={} subs={}", uploadId, subs != null ? subs.size() : 0);
        return casProcessingService.integrateSubtitles(uploadId, subs);
    }

    private static IllegalStateException notOnAnalysisQueue(String method) {
        return new IllegalStateException("CAS only serves analysis-queue; unexpected call: " + method);
    }
}
