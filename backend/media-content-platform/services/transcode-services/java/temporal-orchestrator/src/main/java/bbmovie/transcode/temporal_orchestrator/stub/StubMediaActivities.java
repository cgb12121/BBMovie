package bbmovie.transcode.temporal_orchestrator.stub;

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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class StubMediaActivities implements MediaActivities {

    @Override
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        log.debug("[stub] analyzeSource uploadId={} {}/{}", uploadId, bucket, key);
        return new MetadataDTO(1920, 1080, 120.0, "h264");
    }

    @Override
    public RungResultDTO encodeResolution(EncodeRequest request) {
        log.debug("[stub] encodeResolution {}", request.resolution());
        String path = "bbmovie-hls/movies/" + request.uploadId() + "/" + request.resolution() + "/playlist.m3u8";
        return new RungResultDTO(request.resolution(), path, true);
    }

    @Override
    public QualityReportDTO validateAndScore(ValidationRequest request) {
        log.debug("[stub] validateAndScore {}", request.renditionLabel());
        return new QualityReportDTO(request.renditionLabel(), true, 95.0, "stub");
    }

    @Override
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        log.debug("[stub] generateMasterManifest rungs={}", rungs.size());
        String first = rungs.stream().filter(RungResultDTO::success).map(RungResultDTO::playlistPath).findFirst()
                .orElse("bbmovie-hls/movies/unknown/1080p/playlist.m3u8");
        String master = first.replaceFirst("/[^/]+/playlist\\.m3u8$", "/master.m3u8");
        return new FinalManifestDTO(master, true);
    }

    @Override
    public SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key) {
        log.debug("[stub] normalizeSubtitle {}", uploadId);
        return new SubtitleJsonDTO(uploadId, "{}");
    }

    @Override
    public SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang) {
        log.debug("[stub] translateSubtitle {}", targetLang);
        return new SubtitleJsonDTO(json.uploadId(), json.jsonPayload());
    }

    @Override
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        log.debug("[stub] integrateSubtitles {}", uploadId);
        return new ManifestUpdateDTO("bbmovie-hls/movies/" + uploadId + "/master.m3u8", true);
    }
}
