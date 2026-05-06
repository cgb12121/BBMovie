package bbmovie.transcode.cas.processing;

import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SubInfo;

import java.util.List;

public interface CasProcessingService {

    MetadataDTO analyzeSource(String uploadId, String bucket, String key);

    FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs);

    ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs);
}
