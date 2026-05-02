package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record FinalManifestDTO(
        String masterPlaylistPath,
        boolean success
) implements Serializable {
}
