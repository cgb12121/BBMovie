package bbmovie.transcode.cas.processing;

import bbmovie.transcode.contracts.dto.FinalManifestDTO;
import bbmovie.transcode.contracts.dto.ManifestUpdateDTO;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.RungResultDTO;
import bbmovie.transcode.contracts.dto.SubInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
/**
 * Stub CAS implementation for non-production environments.
 *
 * <p>Returns deterministic placeholder outputs so workflow wiring can be exercised without
 * ffprobe/minio dependencies.</p>
 */
public class CasStubProcessingService implements CasProcessingService {

    @Override
    /** Returns fixed metadata representative of a valid HD source. */
    public MetadataDTO analyzeSource(String uploadId, String bucket, String key) {
        log.debug("[cas-stub] analyzeSource uploadId={} {}/{}", uploadId, bucket, key);
        return new MetadataDTO(1920, 1080, 120.0, "h264");
    }

    @Override
    /** Synthesizes master path from first successful rung, or a fallback sample path. */
    public FinalManifestDTO generateMasterManifest(List<RungResultDTO> rungs) {
        log.debug("[cas-stub] generateMasterManifest rungs={}", rungs.size());
        String first = rungs.stream().filter(RungResultDTO::success).map(RungResultDTO::playlistPath).findFirst()
                .orElse("bbmovie-hls/movies/unknown/1080p/playlist.m3u8");
        String master = first.replaceFirst("/[^/]+/playlist\\.m3u8$", "/master.m3u8");
        return new FinalManifestDTO(master, true);
    }

    @Override
    /** Reports subtitle integration success without mutating remote storage. */
    public ManifestUpdateDTO integrateSubtitles(String uploadId, List<SubInfo> subs) {
        log.debug("[cas-stub] integrateSubtitles uploadId={} subs={}", uploadId, subs.size());
        return new ManifestUpdateDTO("bbmovie-hls/movies/" + uploadId + "/master.m3u8", true);
    }
}
