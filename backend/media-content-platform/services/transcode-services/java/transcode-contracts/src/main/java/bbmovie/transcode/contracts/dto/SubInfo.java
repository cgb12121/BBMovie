package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record SubInfo(
        String language,
        String objectKey
) implements Serializable {
}
