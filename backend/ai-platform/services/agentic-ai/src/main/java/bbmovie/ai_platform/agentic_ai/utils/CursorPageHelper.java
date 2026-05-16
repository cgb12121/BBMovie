package bbmovie.ai_platform.agentic_ai.utils;

import com.bbmovie.common.dtos.CursorPageResponse;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.function.Function;

/**
 * Generic utility for cursor-based pagination.
 *
 * <p>Eliminates the repeated "fetch size+1, check hasNext, slice, extract cursor" pattern
 * found in {@code SessionService} and {@code MessageService}.
 *
 * <p>Usage example:
 * <pre>
 *   return sessionFlux.collectList().map(sessions ->
 *       CursorPageHelper.toPage(sessions, size,
 *           s -> s.getCreatedAt().toString(),
 *           s -> new ChatSessionResponse(s.getId(), s.getName(), ...))
 *   );
 * </pre>
 *
 * @param <T> The source entity type (e.g., {@code ChatSession}).
 * @param <R> The response DTO type (e.g., {@code ChatSessionResponse}).
 */
@UtilityClass
public class CursorPageHelper {

    /**
     * Converts a raw over-fetched list into a {@link CursorPageResponse}.
     *
     * <p>The caller is expected to fetch {@code size + 1} items from the DB.
     * This method uses the extra item to determine whether a next page exists
     * and extracts the cursor from the last item of the actual page.
     *
     * @param items         The raw list, containing at most {@code size + 1} elements.
     * @param size          The requested page size.
     * @param cursorExtractor A function that extracts the cursor string from an entity
     *                        (typically the ISO-8601 string of a timestamp).
     * @param mapper          A function that maps the source entity to its response DTO.
     * @return A fully populated {@link CursorPageResponse}.
     */
    public static <T, R> CursorPageResponse<R> toPage(
            List<T> items,
            int size,
            Function<T, String> cursorExtractor,
            Function<T, R> mapper) {

        boolean hasNext = items.size() > size;
        List<T> pageItems = hasNext ? items.subList(0, size) : items;

        String nextCursor = pageItems.isEmpty()
                ? null
                : cursorExtractor.apply(pageItems.get(pageItems.size() - 1));

        List<R> responses = pageItems.stream()
                .map(mapper)
                .toList();

        return new CursorPageResponse<>(responses, nextCursor, hasNext, size);
    }
}
