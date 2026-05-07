package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Input payload for rendition validation/quality scoring activity.
 *
 * @param uploadId logical upload identifier
 * @param playlistPath object key/path of rendition playlist to validate
 * @param sourceBucket source object bucket containing original reference media
 * @param sourceKey source object key/path for original reference media
 * @param renditionLabel rendition name used for reporting (e.g. {@code 720p})
 * @param expectedWidth expected encoded width in pixels
 * @param expectedHeight expected encoded height in pixels
 * @param expectedVideoCodec expected ffprobe {@code codec_name} for video (e.g. {@code h264}, {@code hevc}, {@code av1})
 * @param expectedAudioCodec expected ffprobe {@code codec_name} for audio (e.g. {@code aac}, {@code eac3}, {@code opus})
 * @param expectedPixFmt expected ffprobe {@code pix_fmt} when known (empty means don't enforce)
 * @param expectedAudioChannels expected audio channel count when known (0 means don't enforce)
 * @param expectedAudioSampleRate expected audio sample rate when known (0 means don't enforce)
 * @param expectedHlsTargetDurationSeconds expected HLS target duration (0 means don't enforce)
 * @param expectedVodPlaylist whether VVS should require {@code #EXT-X-ENDLIST} (VOD)
 */
public record ValidationRequest(
        String uploadId,
        String playlistPath,
        String sourceBucket,
        String sourceKey,
        String renditionLabel,
        int expectedWidth,
        int expectedHeight,
        String expectedVideoCodec,
        String expectedAudioCodec,
        String expectedPixFmt,
        int expectedAudioChannels,
        int expectedAudioSampleRate,
        int expectedHlsTargetDurationSeconds,
        boolean expectedVodPlaylist
) implements Serializable {

    public ValidationRequest(
            String uploadId,
            String playlistPath,
            String sourceBucket,
            String sourceKey,
            String renditionLabel,
            int expectedWidth,
            int expectedHeight
    ) {
        this(
                uploadId,
                playlistPath,
                sourceBucket,
                sourceKey,
                renditionLabel,
                expectedWidth,
                expectedHeight,
                "",
                "",
                "",
                0,
                0,
                0,
                true
        );
    }
}
