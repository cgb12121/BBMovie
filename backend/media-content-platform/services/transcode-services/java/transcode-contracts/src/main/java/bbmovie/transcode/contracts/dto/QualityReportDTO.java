package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Validation result for one rendition.
 *
 * @param renditionLabel rendition identifier used in validation context
 * @param passed whether rendition passed validation thresholds
 * @param score numeric signal value (VQS quality score or neutral value for VVS validation)
 * @param detail detail/reason message for pass/fail interpretation
 * @param vmafMean mean VMAF score when available
 * @param vmafP10 VMAF p10 score when available
 * @param vmafWorstWindow minimum rolling-window VMAF score when available
 * @param qualityReasonCode machine-readable reason code (e.g. vvs_* or quality gate codes from VQS)
 */
public record QualityReportDTO(
        String renditionLabel,
        boolean passed,
        double score,
        String detail,
        Double vmafMean,
        Double vmafP10,
        Double vmafWorstWindow,
        String qualityReasonCode
) implements Serializable {

    public QualityReportDTO(String renditionLabel, boolean passed, double score, String detail) {
        this(renditionLabel, passed, score, detail, null, null, null, "");
    }
}
