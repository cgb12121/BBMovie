package bbmovie.transcode.cas.analysis;

public enum ComplexityRiskClass {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME;

    /** Maps normalized complexity score into coarse policy risk bucket. */
    public static ComplexityRiskClass fromScore(double score) {
        if (score >= 0.85) {
            return EXTREME;
        }
        if (score >= 0.65) {
            return HIGH;
        }
        if (score >= 0.40) {
            return MEDIUM;
        }
        return LOW;
    }
}
