package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.SourceProfileV2;

public interface ComplexityAnalysisV2Service {

    ComplexityProfileV2 analyze(SourceProfileV2 sourceProfile);
}
