package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record EncodeRequest(
        String uploadId,
        String resolution,
        int width,
        int height,
        String masterKeyHex,
        String masterIvHex,
        String sourceBucket,
        String sourceKey
) implements Serializable {
}
