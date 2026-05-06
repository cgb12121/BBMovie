package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record ManifestUpdateDTO(
        String masterPlaylistPath,
        boolean updated
) implements Serializable {
}
