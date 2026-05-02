package bbmovie.transcode.vqs.activity;

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
import bbmovie.transcode.vqs.processing.VqsQualityProcessingService;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VqsMediaActivities implements MediaActivities {

    private final VqsQualityProcessingService vqsQualityProcessingService;

    @Override
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnQualityQueue("analyzeSource"));
    }

    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        throw Activity.wrap(notOnQualityQueue("encodeResolution"));
    }

    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        log.debug("[vqs] validateAndScore {}", request.renditionLabel());
        return vqsQualityProcessingService.validateAndScore(request);
    }

    @Override
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

    private static IllegalStateException notOnQualityQueue(String method) {
        return new IllegalStateException("VQS worker only serves quality-queue; unexpected call: " + method);
    }
}
