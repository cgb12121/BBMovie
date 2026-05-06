package bbmovie.transcode.cas.processing;

import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SubInfo;

import java.util.List;

/** CAS business operations invoked by Temporal activity adapter. */
public interface CasProcessingService {

    /** Analyze source media and produce metadata payload for workflow decisions. */
    MetadataDTO analyzeSource(String uploadId, String bucket, String key);

    /** Build and upload master playlist from successful encode rungs. */
    FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs);

    /** Add subtitle renditions into the uploaded master playlist. */
    ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs);
}
