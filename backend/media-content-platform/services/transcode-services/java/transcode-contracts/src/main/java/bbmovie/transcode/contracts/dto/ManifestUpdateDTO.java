package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Outcome of master manifest subtitle-integration update.
 *
 * @param masterPlaylistPath object path/key of updated master playlist
 * @param updated whether update operation produced/persisted changes
 */
public record ManifestUpdateDTO(
        String masterPlaylistPath,
        boolean updated
) implements Serializable {
}
