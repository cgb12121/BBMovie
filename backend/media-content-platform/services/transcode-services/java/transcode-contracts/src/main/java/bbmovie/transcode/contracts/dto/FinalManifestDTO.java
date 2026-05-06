package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Outcome of master manifest generation.
 *
 * @param masterPlaylistPath object path/key of generated master playlist
 * @param success whether master manifest creation/upload succeeded
 */
public record FinalManifestDTO(
        String masterPlaylistPath,
        boolean success
) implements Serializable {
}
