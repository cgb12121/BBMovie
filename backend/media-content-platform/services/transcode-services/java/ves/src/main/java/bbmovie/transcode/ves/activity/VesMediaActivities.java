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
import io.temporal.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class VesMediaActivities implements MediaActivities {

    @Override
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnEncodingQueue("analyzeSource"));
    }

    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        log.debug("[ves] encodeResolution {}", request.resolution());
        String path = "bbmovie-hls/movies/" + request.uploadId() + "/" + request.resolution() + "/playlist.m3u8";
        return new RungResultDTO(request.resolution(), path, true);
    }

    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        throw Activity.wrap(notOnEncodingQueue("validateAndScore"));
    }

    @Override
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        throw Activity.wrap(notOnEncodingQueue("generateMasterManifest"));
    }

    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        throw Activity.wrap(notOnEncodingQueue("normalizeSubtitle"));
    }

    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        throw Activity.wrap(notOnEncodingQueue("translateSubtitle"));
    }

    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        throw Activity.wrap(notOnEncodingQueue("integrateSubtitles"));
    }

    private static IllegalStateException notOnEncodingQueue(String method) {
        return new IllegalStateException("VES only serves encoding-queue; unexpected call: " + method);
    }
}
