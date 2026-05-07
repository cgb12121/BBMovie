package bbmovie.transcode.vis.probe;

import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import bbmovie.transcode.contracts.dto.VisDecisionReportDTO;
import org.springframework.stereotype.Component;

@Component
public class SourceProfileCompatibilityAdapter {

    public MetadataDTO toMetadataDTO(VisProfileV2Service.AnalysisResult analysisResult) {
        if (analysisResult == null) {
            throw new IllegalArgumentException("analysisResult must not be null");
        }
        return toMetadataDTO(analysisResult.sourceProfile(), analysisResult.decisionReport());
    }

    public MetadataDTO toMetadataDTO(SourceProfileV2 sourceProfileV2) {
        return toMetadataDTO(sourceProfileV2, null);
    }

    public MetadataDTO toMetadataDTO(SourceProfileV2 sourceProfileV2, VisDecisionReportDTO visDecisionReport) {
        return new MetadataDTO(
                sourceProfileV2.width(),
                sourceProfileV2.height(),
                sourceProfileV2.durationSeconds(),
                sourceProfileV2.codec(),
                sourceProfileV2,
                null,
                null,
                visDecisionReport
        );
    }
}
