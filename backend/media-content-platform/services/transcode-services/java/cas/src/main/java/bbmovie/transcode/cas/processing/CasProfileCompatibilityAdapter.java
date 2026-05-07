package bbmovie.transcode.cas.processing;

import bbmovie.transcode.cas.dto.ComplexityProfile;
import bbmovie.transcode.cas.dto.RecipeHints;
import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Converts between v1 and v2 CAS profile/metadata shapes.
 *
 * <p>Used during migration so legacy workflow paths can continue consuming v1 objects while
 * newer orchestration paths consume v2 contracts.</p>
 */
@Component
public class CasProfileCompatibilityAdapter {

    /** Produces a minimal metadata payload when complexity data is unavailable. */
    public MetadataDTO toMetadataDto(SourceProfileV2 sourceProfile) {
        return new MetadataDTO(
                sourceProfile.width(),
                sourceProfile.height(),
                sourceProfile.durationSeconds(),
                sourceProfile.codec()
        );
    }

    /** Produces metadata embedding full source + complexity v2 details. */
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

    /** Maps v2 fields into the legacy v1 profile contract used by existing ladder logic. */
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
    }
}
