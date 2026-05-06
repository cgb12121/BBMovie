package bbmovie.transcode.vis.probe;

/**
 * Ported from transcode-worker {@code ProbeStrategy}.
 */
public interface VisProbeStrategy {

    String getName();

    boolean supports(String bucket, String key);

    VisProbeOutcome probe(String bucket, String key) throws VisProbeException;

    default int getPriority() {
        return 0;
    }

    class VisProbeException extends RuntimeException {
        public VisProbeException(String message) {
            super(message);
        }

        public VisProbeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
