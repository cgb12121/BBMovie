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
public class CasMediaActivities implements MediaActivities {

    private final CasProcessingService casProcessingService;

    @Override
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        log.debug("[cas] analyzeSource uploadId={} {}/{}", uploadId, bucket, key);
        return casProcessingService.analyzeSource(uploadId, bucket, key);
    }

    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        throw Activity.wrap(notOnAnalysisQueue("encodeResolution"));
    }

    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        throw Activity.wrap(notOnAnalysisQueue("validateAndScore"));
    }

    @Override
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        log.debug("[cas] generateMasterManifest rungs={}", rungs.size());
        return casProcessingService.generateMasterManifest(rungs);
    }

    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnAnalysisQueue("normalizeSubtitle"));
    }

    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        throw Activity.wrap(notOnAnalysisQueue("translateSubtitle"));
    }

    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        log.debug("[cas] integrateSubtitles uploadId={} subs={}", uploadId, subs != null ? subs.size() : 0);
        return casProcessingService.integrateSubtitles(uploadId, subs);
    }

    private static IllegalStateException notOnAnalysisQueue(String method) {
        return new IllegalStateException("CAS only serves analysis-queue; unexpected call: " + method);
    }
}
