package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record ValidationRequest(
        String uploadId,
        String playlistPath,
        String renditionLabel,
        int expectedWidth,
        int expectedHeight
) implements Serializable {
}
