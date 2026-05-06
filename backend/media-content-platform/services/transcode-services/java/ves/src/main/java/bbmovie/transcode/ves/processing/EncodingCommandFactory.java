package bbmovie.transcode.ves.processing;

import bbmovie.transcode.contracts.dto.EncodeBitrateStrategy;
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
                .done()
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

        EncodeBitrateStrategy mode = request.bitrateStrategy();
        if (mode == null) {
            mode = EncodeBitrateStrategy.DEFAULT;
        }
        switch (mode) {
            case VBV_ABR -> {
                Integer maxK = request.maxBitrateKbps();
                if (maxK != null && maxK > 0) {
                    Integer minK = request.minBitrateKbps();
                    int lo = minK != null && minK > 0 ? minK : Math.max(1, maxK / 4);
                    int hi = maxK;
                    int target = (lo + hi) / 2;
                    target = Math.max(lo, Math.min(hi, target));
                    output.addExtraArgs("-b:v", target + "k");
                    if (minK != null && minK > 0) {
                        output.addExtraArgs("-minrate", minK + "k");
                    }
                    output.addExtraArgs("-maxrate", maxK + "k");
                    output.addExtraArgs("-bufsize", (maxK * 2) + "k");
                }
            }
            case VBV_CRF_CAP -> {
                Integer maxK = request.maxBitrateKbps();
                if (maxK != null && maxK > 0) {
                    int crf = request.encoderCrf() != null ? request.encoderCrf() : 23;
                    output.addExtraArgs("-crf", Integer.toString(crf));
                    output.addExtraArgs("-maxrate", maxK + "k");
                    output.addExtraArgs("-bufsize", (maxK * 2) + "k");
                }
            }
            case DEFAULT -> {
            }
        }
        if (request.conservativeMode()) {
            output.addExtraArgs("-x264-params", "scenecut=0:open-gop=0");
        }
        
        return output.done();
    }
}
