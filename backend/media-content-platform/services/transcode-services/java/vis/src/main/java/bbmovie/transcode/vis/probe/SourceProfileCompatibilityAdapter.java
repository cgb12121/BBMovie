package bbmovie.transcode.vis.probe;

import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import org.springframework.stereotype.Component;

@Component
public class SourceProfileCompatibilityAdapter {

    public MetadataDTO toMetadataDTO(SourceProfileV2 sourceProfileV2) {
        return new MetadataDTO(
                sourceProfileV2.width(),
                sourceProfileV2.height(),
                sourceProfileV2.durationSeconds(),
                sourceProfileV2.codec()
        );
    }
}
