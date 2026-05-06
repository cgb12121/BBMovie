package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record QualityReportDTO(
        String renditionLabel,
        boolean passed,
        double score,
        String detail
) implements Serializable {
}
