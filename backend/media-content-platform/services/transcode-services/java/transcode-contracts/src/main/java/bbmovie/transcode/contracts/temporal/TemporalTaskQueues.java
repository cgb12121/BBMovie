package bbmovie.transcode.contracts.temporal;

/** Canonical Temporal task-queue names shared across transcode services. */
public final class TemporalTaskQueues {

    /** Orchestrator workflow task queue. */
    public static final String ORCHESTRATOR = "orchestrator-queue";
    /** Analyzer activities task queue (probe/analysis/planning). */
    public static final String ANALYSIS = "analysis-queue";
    /** Encoder activities task queue. */
    public static final String ENCODING = "encoding-queue";
    /** Validation/conformance activities task queue. */
    public static final String VALIDATION = "validation-queue";
    /** Perceptual quality scoring activities task queue. */
    public static final String QUALITY = "quality-queue";
    /** Subtitle processing activities task queue. */
    public static final String SUBTITLE = "subtitle-queue";

    private TemporalTaskQueues() {
    }
}
