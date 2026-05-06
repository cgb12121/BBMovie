package bbmovie.transcode.contracts.temporal;

/** Canonical Temporal task-queue names shared across transcode services. */
public final class TemporalTaskQueues {

    /** Orchestrator workflow task queue. */
    public static final String ORCHESTRATOR = "orchestrator-queue";
    /** Analyzer activities task queue (probe/analysis/planning). */
    public static final String ANALYSIS = "analysis-queue";
    /** Encoder activities task queue. */
    public static final String ENCODING = "encoding-queue";
    /** Quality/validation activities task queue. */
    public static final String QUALITY = "quality-queue";
    /** Subtitle processing activities task queue. */
    public static final String SUBTITLE = "subtitle-queue";

    private TemporalTaskQueues() {
    }
}
