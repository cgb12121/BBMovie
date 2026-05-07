package bbmovie.transcode.lgs.dto;

import java.util.Set;

/**
 * Hint payload that tunes LGS ladder selection.
 *
 * <p>Ported from transcode-worker {@code RecipeHints}.</p>
 */
public record LgsRecipeHints(Set<String> skipSuffixes, double bitrateBias, int maxRungs, int minRungs) {

    /** No-op hints: no skipped rungs and neutral bitrate bias. */
    public static LgsRecipeHints none() {
        return new LgsRecipeHints(Set.of(), 1.0, 6, 1);
    }

    /** Convenience constructor for skip-only hints with neutral bitrate bias. */
    public static LgsRecipeHints skip(Set<String> suffixes) {
        return new LgsRecipeHints(Set.copyOf(suffixes), 1.0, 6, 1);
    }
}
