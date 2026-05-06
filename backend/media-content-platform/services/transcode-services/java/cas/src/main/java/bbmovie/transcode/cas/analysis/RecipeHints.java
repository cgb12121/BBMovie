package bbmovie.transcode.cas.analysis;

import java.util.Set;

/**
 * CAS output consumed by ladder tuning (ported from transcode-worker {@code RecipeHints}).
 */
public record RecipeHints(Set<String> skipSuffixes, double bitrateBias) {

    /** No-op hints: do not skip rungs and keep neutral bitrate bias. */
    public static RecipeHints none() {
        return new RecipeHints(Set.of(), 1.0);
    }

    /** Convenience constructor for skip-only hints with neutral bitrate bias. */
    public static RecipeHints skip(Set<String> suffixes) {
        return new RecipeHints(Set.copyOf(suffixes), 1.0);
    }
}
