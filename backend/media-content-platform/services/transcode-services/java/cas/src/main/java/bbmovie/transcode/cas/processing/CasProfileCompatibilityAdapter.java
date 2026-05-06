package bbmovie.transcode.cas.processing;

import bbmovie.transcode.cas.analysis.ComplexityProfile;
import bbmovie.transcode.cas.analysis.RecipeHints;
import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;

@Component
public class CasProfileCompatibilityAdapter {

    public MetadataDTO toMetadataDto(SourceProfileV2 sourceProfile) {
        return new MetadataDTO(
                sourceProfile.width(),
                sourceProfile.height(),
                sourceProfile.durationSeconds(),
                sourceProfile.codec()
        );
    }

    public MetadataDTO toMetadataDto(SourceProfileV2 sourceProfile, ComplexityProfileV2 complexityProfileV2) {
        return new MetadataDTO(
                sourceProfile.width(),
                sourceProfile.height(),
                sourceProfile.durationSeconds(),
                sourceProfile.codec(),
                sourceProfile,
                complexityProfileV2,
                complexityProfileV2 != null ? complexityProfileV2.decisionHints() : null
        );
    }

    public ComplexityProfile toLegacyComplexityProfile(ComplexityProfileV2 complexityProfileV2) {
        String riskClass = complexityProfileV2.riskClass();
        var decisionHints = complexityProfileV2.decisionHints();
        return new ComplexityProfile(
                complexityProfileV2.uploadId(),
                riskClass != null ? riskClass.toLowerCase() : "unknown",
                complexityProfileV2.complexityScore(),
                new HashMap<>(complexityProfileV2.dimensionScores()),
                RecipeHints.skip(decisionHints != null && decisionHints.skipRungs() != null
                        ? new HashSet<>(decisionHints.skipRungs())
                        : new HashSet<>()),
                complexityProfileV2.analyzedAt()
        );
    }}
