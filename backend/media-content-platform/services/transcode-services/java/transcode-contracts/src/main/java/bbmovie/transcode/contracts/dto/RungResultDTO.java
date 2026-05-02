package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record RungResultDTO(
        String resolution,
        String playlistPath,
        boolean success
) implements Serializable {
}
