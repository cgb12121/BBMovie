package bbmovie.transcode.vis.probe;

import bbmovie.transcode.vis.dto.VisProbeOutcome;

/**
 * Contract for VIS probe strategies.
 *
 * <p>Each strategy declares support + priority and returns normalized probe outcome.</p>
 */
public interface VisProbeStrategy {

    /** Human-readable strategy name for logs and diagnostics. */
    String getName();

    /** Whether strategy can probe the given object key/bucket combination. */
    boolean supports(String bucket, String key);

    /** Executes probing and returns normalized outcome. */
    VisProbeOutcome probe(String bucket, String key) throws VisProbeException;

    /** Higher value means earlier execution order in fast-probe coordinator. */
    default int getPriority() {
        return 0;
    }

    /** Strategy-level probe failure abstraction. */
    class VisProbeException extends RuntimeException {
        public VisProbeException(String message) {
            super(message);
        }

        public VisProbeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
