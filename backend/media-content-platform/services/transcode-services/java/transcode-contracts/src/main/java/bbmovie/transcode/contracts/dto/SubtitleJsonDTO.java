package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Normalized subtitle payload exchanged between subtitle activities.
 *
 * @param uploadId logical upload identifier subtitle belongs to
 * @param jsonPayload serialized subtitle content in normalized JSON form
 */
public record SubtitleJsonDTO(
        String uploadId,
        String jsonPayload
) implements Serializable {
}
