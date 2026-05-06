package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record SubtitleJsonDTO(
        String uploadId,
        String jsonPayload
) implements Serializable {
}
