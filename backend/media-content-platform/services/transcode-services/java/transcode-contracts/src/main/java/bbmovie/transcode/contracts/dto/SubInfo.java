package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Subtitle track descriptor used when integrating subtitle playlists into master manifest.
 *
 * @param language language tag/label for subtitle track (e.g. {@code en}, {@code vi})
 * @param objectKey object key/path to subtitle playlist/file
 */
public record SubInfo(
        String language,
        String objectKey
) implements Serializable {
}
