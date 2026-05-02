package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record MetadataDTO(
        int width,
        int height,
        double durationSeconds,
        String codec
) implements Serializable {
}
