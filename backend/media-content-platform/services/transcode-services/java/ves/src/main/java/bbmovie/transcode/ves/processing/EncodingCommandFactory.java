package bbmovie.transcode.ves.processing;

import bbmovie.transcode.contracts.dto.EncodeBitrateStrategy;
import bbmovie.transcode.contracts.dto.EncodeRequest;
import bbmovie.transcode.ves.config.MediaProcessingProperties;
import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;

import java.nio.file.Path;

@RequiredArgsConstructor
/**
 * Builds FFmpeg command graph for one VES HLS rendition encode.
 *
 * <p>Encapsulates policy-driven bitrate strategy handling so workflow/activity layers stay free of
 * FFmpeg argument details.</p>
 */
public class EncodingCommandFactory {

    private final MediaProcessingProperties properties;

    /**
     * Creates FFmpeg builder configured for HLS output and optional bitrate policy hints.
     *
     * @param request encode request containing target dimensions and bitrate strategy hints
     * @param sourceUrl presigned source URL consumed directly by ffmpeg input
     * @param playlist target output playlist path in local temp workspace
     * @param segmentPattern ffmpeg segment filename pattern path
     * @return finalized FFmpeg builder ready for {@code FFmpegExecutor#createJob}
     */
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
                // ABR mode: center target bitrate between policy min/max with VBV guardrails.
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
                // CRF mode: quality-targeted encoding capped by maxrate/bufsize for bandwidth control.
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
            // Conservative mode disables scenecut/open-gop to reduce segment variance/drift risk.
            output.addExtraArgs("-x264-params", "scenecut=0:open-gop=0");
        }
        
        return output.done();
    }
}
