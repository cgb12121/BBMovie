package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Result of one rendition encode attempt.
 *
 * @param resolution rendition label (e.g. {@code 1080p})
 * @param playlistPath object path/key of generated rendition playlist
 * @param success whether encode/upload for this rung completed successfully
 */
public record RungResultDTO(
        String resolution,
        String playlistPath,
        boolean success
) implements Serializable {
}
