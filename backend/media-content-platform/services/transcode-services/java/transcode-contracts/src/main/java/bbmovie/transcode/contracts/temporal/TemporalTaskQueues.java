package bbmovie.transcode.contracts.temporal;

public final class TemporalTaskQueues {

    public static final String ORCHESTRATOR = "orchestrator-queue";
    public static final String ANALYSIS = "analysis-queue";
    public static final String ENCODING = "encoding-queue";
    public static final String QUALITY = "quality-queue";
    public static final String SUBTITLE = "subtitle-queue";

    private TemporalTaskQueues() {
    }
}
