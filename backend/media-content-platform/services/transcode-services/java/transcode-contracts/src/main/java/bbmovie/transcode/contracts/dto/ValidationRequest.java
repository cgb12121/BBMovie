package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Input payload for rendition validation/quality scoring activity.
 *
 * @param uploadId logical upload identifier
 * @param playlistPath object key/path of rendition playlist to validate
 * @param renditionLabel rendition name used for reporting (e.g. {@code 720p})
 * @param expectedWidth expected encoded width in pixels
 * @param expectedHeight expected encoded height in pixels
 */
public record ValidationRequest(
        String uploadId,
        String playlistPath,
        String renditionLabel,
        int expectedWidth,
        int expectedHeight
) implements Serializable {
}
