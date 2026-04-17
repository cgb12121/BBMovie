package com.bbmovie.transcodeworker.service.complexity.dto;

import java.util.Set;

/**
 * CAS output consumed by LGS to prune or tune the ladder.
 */
public record RecipeHints(Set<String> skipSuffixes, double bitrateBias) {

    public static RecipeHints none() {
        return new RecipeHints(Set.of(), 1.0);
    }

    public static RecipeHints skip(Set<String> suffixes) {
        return new RecipeHints(Set.copyOf(suffixes), 1.0);
    }
}
