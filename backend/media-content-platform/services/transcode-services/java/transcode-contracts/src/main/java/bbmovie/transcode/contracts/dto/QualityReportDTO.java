package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Validation result for one rendition.
 *
 * @param renditionLabel rendition identifier used in validation context
 * @param passed whether rendition passed validation thresholds
 * @param score numeric quality score (implementation-specific scale)
 * @param detail detail/reason message for pass/fail interpretation
 */
public record QualityReportDTO(
        String renditionLabel,
        boolean passed,
        double score,
        String detail
) implements Serializable {
}
