package bbmovie.transcode.lgs.analysis;

import java.util.Set;

/**
 * Ported from transcode-worker {@code RecipeHints}.
 */
public record LgsRecipeHints(Set<String> skipSuffixes, double bitrateBias) {

    public static LgsRecipeHints none() {
        return new LgsRecipeHints(Set.of(), 1.0);
    }

    public static LgsRecipeHints skip(Set<String> suffixes) {
        return new LgsRecipeHints(Set.copyOf(suffixes), 1.0);
    }
}
