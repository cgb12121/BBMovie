package bbmovie.transcode.cas.analysis;

import java.util.Set;

/**
 * CAS output consumed by ladder tuning (ported from transcode-worker {@code RecipeHints}).
 */
public record RecipeHints(Set<String> skipSuffixes, double bitrateBias) {

    public static RecipeHints none() {
        return new RecipeHints(Set.of(), 1.0);
    }

    public static RecipeHints skip(Set<String> suffixes) {
        return new RecipeHints(Set.copyOf(suffixes), 1.0);
    }
}
