package bbmovie.transcode.contracts.activity;

import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.QualityReportDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SubInfo;
import bbmovie.transcode.contracts.dto.SubtitleJsonDTO;
import bbmovie.transcode.contracts.dto.ValidationRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

@ActivityInterface(namePrefix = "TranscodeMedia.")
public interface MediaActivities {

    @ActivityMethod
    MetadataDTO analyzeSource(String uploadId, String bucket, String key);

    @ActivityMethod
    RungResultDTO encodeResolution(EncodeRequest request);

    @ActivityMethod
    QualityReportDTO validateAndScore(ValidationRequest request);

    @ActivityMethod
    FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs);

    @ActivityMethod
    SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key);

    @ActivityMethod
    SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang);

    @ActivityMethod
    ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs);
}
