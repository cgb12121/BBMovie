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

/** Shared Temporal activity contract for media transcode workflow stages. */
@ActivityInterface(namePrefix = "TranscodeMedia.")
public interface MediaActivities {

    @ActivityMethod
    /**
     * Analyze source object and return metadata used for downstream planning.
     *
     * @param uploadId logical upload identifier
     * @param bucket source object bucket
     * @param key source object key/path inside bucket
     * @return analyzed source metadata payload
     */
    MetadataDTO analyzeSource(String uploadId, String bucket, String key);

    @ActivityMethod
    /**
     * Encode one rendition described by {@link EncodeRequest}.
     *
     * @param request encode input payload with source path and target rendition settings
     * @return rendition encode result
     */
    RungResultDTO encodeResolution(EncodeRequest request);

    @ActivityMethod
    /**
     * Validate one rendition and return quality score/report.
     *
     * @param request validation input with expected dimensions and rendition path
     * @return quality report for requested rendition
     */
    QualityReportDTO validateAndScore(ValidationRequest request);

    @ActivityMethod
    /**
     * Build a master playlist from completed rendition outputs.
     *
     * @param rungs successful/failed rendition outputs from encode stage
     * @return master manifest creation result
     */
    FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs);

    @ActivityMethod
    /**
     * Normalize subtitle source into canonical JSON payload.
     *
     * @param uploadId logical upload identifier
     * @param bucket subtitle object bucket
     * @param key subtitle object key/path
     * @return normalized subtitle JSON payload
     */
    SubtitleJsonDTO normalizeSubtitle(String uploadId, String bucket, String key);

    @ActivityMethod
    /**
     * Translate normalized subtitle payload into target language.
     *
     * @param json normalized subtitle JSON payload
     * @param targetLang target language tag/code
     * @return translated subtitle JSON payload
     */
    SubtitleJsonDTO translateSubtitle(SubtitleJsonDTO json, String targetLang);

    @ActivityMethod
    /**
     * Integrate subtitle tracks into existing master playlist.
     *
     * @param uploadId logical upload identifier
     * @param subs subtitle track descriptors to inject
     * @return manifest update result
     */
    ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs);
}
