package bbmovie.transcode.vis.probe;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Coordinator that executes VIS probe strategies by priority until one succeeds.
 *
 * <p>Strategy failures are tolerated and fallback to next candidate; only all-failed case throws.</p>
 */
@Slf4j
@Service
public class VisFastProbeService {

    private final List<VisProbeStrategy> strategies;

    public VisFastProbeService(List<VisProbeStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(VisProbeStrategy::getPriority).reversed())
                .toList();
    }

    @PostConstruct
    /** Logs enabled strategy order for runtime observability. */
    public void init() {
        log.info("VIS FastProbeService strategies: {}", strategies.stream().map(VisProbeStrategy::getName).toList());
    }

    /** Attempts probing through supported strategies in descending priority order. */
    public VisProbeOutcome probe(String bucket, String key) {
        Exception last = null;
        for (VisProbeStrategy strategy : strategies) {
            if (!strategy.supports(bucket, key)) {
                continue;
            }
            try {
                return strategy.probe(bucket, key);
            } catch (Exception e) {
                log.warn("Strategy {} failed for {}/{}: {}", strategy.getName(), bucket, key, e.getMessage());
                last = e;
            }
        }
        throw new VisProbeStrategy.VisProbeException("All VIS probe strategies failed for " + bucket + "/" + key, last);
    }
}
