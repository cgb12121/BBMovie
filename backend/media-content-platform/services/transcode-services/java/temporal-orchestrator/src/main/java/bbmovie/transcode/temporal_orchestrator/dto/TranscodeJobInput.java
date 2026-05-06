package bbmovie.transcode.temporal_orchestrator.dto;

import java.io.Serializable;

public record TranscodeJobInput(
        String uploadId,
        String bucket,
        String key,
        UploadPurpose purpose,
        String contentType,
        long fileSizeBytes
) implements Serializable {
}
