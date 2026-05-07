package bbmovie.transcode.ves.activity;

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
import bbmovie.transcode.ves.processing.EncodingProcessingService;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Temporal activity adapter for VES encoding queue.
 *
 * <p>This worker is intentionally queue-scoped: it serves only encode requests and rejects
 * analyzer/quality/subtitle operations to keep workflow routing explicit and safe.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncodingActivities implements MediaActivities {

    private final EncodingProcessingService encodingProcessingService;

    @Override
    /** Unsupported on encoding queue; source analysis belongs to analyzer workers. */
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnEncodingQueue("analyzeSource"));
    }

    /**
     * Encodes one rendition using stream-based VES pipeline.
     *
     * @param request encode payload containing source object + rendition constraints
     * @return encode result with rendition label, output playlist path, and success flag
     */
    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        log.debug("[ves] encodeResolution {}", request.resolution());
        return encodingProcessingService.encodeResolution(request);
    }

    /** Unsupported on encoding queue; validation belongs to quality workers. */
    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        throw Activity.wrap(notOnEncodingQueue("validateAndScore"));
    }

    /** Unsupported on encoding queue; manifest generation belongs to analyzer/orchestrator path. */
    @Override
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        throw Activity.wrap(notOnEncodingQueue("generateMasterManifest"));
    }

    /** Unsupported on encoding queue; subtitle normalization is handled elsewhere. */
    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnEncodingQueue("normalizeSubtitle"));
    }

    /** Unsupported on encoding queue; subtitle translation is handled elsewhere. */
    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        throw Activity.wrap(notOnEncodingQueue("translateSubtitle"));
    }

    /** Unsupported on encoding queue; subtitle integration belongs to manifest/subtitle flow. */
    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        throw Activity.wrap(notOnEncodingQueue("integrateSubtitles"));
    }

    /** Shared fail-fast helper for operations outside encoding queue responsibilities. */
    private static IllegalStateException notOnEncodingQueue(String method) {
        return new IllegalStateException("VES only serves encoding-queue; unexpected call: " + method);
    }
}
