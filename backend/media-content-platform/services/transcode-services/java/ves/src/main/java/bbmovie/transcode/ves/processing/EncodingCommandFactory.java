package bbmovie.transcode.ves.processing;

import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.ves.config.MediaProcessingProperties;
import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;

import java.nio.file.Path;

@RequiredArgsConstructor
public class EncodingCommandFactory {

    private final MediaProcessingProperties properties;

    public FFmpegBuilder buildHlsStreamEncode(EncodeRequest request, String sourceUrl, Path playlist, Path segmentPattern) {
        String preset = request.preferredPreset() != null && !request.preferredPreset().isBlank()
                ? request.preferredPreset()
                : "veryfast";

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(sourceUrl)
                .overrideOutputFiles(true);

        FFmpegOutputBuilder output = builder.addOutput(playlist.toString())
                .setVideoCodec("libx264")
                .setPreset(preset)
                .setAudioCodec("aac")
                .setAudioBitRate(128_000)
                .setVideoFilter("scale=" + request.width() + ":-2")
                .setFormat("hls")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", segmentPattern.toString())
                .addExtraArgs("-hls_playlist_type", "vod")
                .addExtraArgs("-threads", Integer.toString(Math.max(1, properties.getFfmpegThreads())));

        if (request.minBitrateKbps() != null && request.minBitrateKbps() > 0) {
            output.addExtraArgs("-minrate", request.minBitrateKbps() + "k");
        }
        if (request.maxBitrateKbps() != null && request.maxBitrateKbps() > 0) {
            output.addExtraArgs("-maxrate", request.maxBitrateKbps() + "k");
            output.addExtraArgs("-bufsize", (request.maxBitrateKbps() * 2) + "k");
        }
        if (request.conservativeMode()) {
            output.addExtraArgs("-x264-params", "scenecut=0:open-gop=0");
        }
        
        return output.done();
    }
}
