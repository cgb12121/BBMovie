package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.SourceProfileV2;

/** Contract for CAS complexity-v2 analyzers that emit policy-ready profile outputs. */
public interface ComplexityAnalysisV2Service {

    /** Analyze normalized source profile and return complexity + decision-hint payload. */
    ComplexityProfileV2 analyze(SourceProfileV2 sourceProfile);
}
