package bbmovie.transcode.contracts.planning;

import java.io.Serializable;
import java.util.List;

/**
 * Shared ladder templates used by VIS/LGS planning paths.
 */
public final class TranscodeLadderTemplates {

    private static final List<LadderPreset> BASELINE_HLS = List.of(
            new LadderPreset(1080, 1920, 1080, "1080p"),
            new LadderPreset(720, 1280, 720, "720p"),
            new LadderPreset(480, 854, 480, "480p"),
            new LadderPreset(360, 640, 360, "360p"),
            new LadderPreset(240, 426, 240, "240p"),
            new LadderPreset(144, 256, 144, "144p")
    );

    private TranscodeLadderTemplates() {
    }

    /**
     * Returns immutable baseline HLS ladder presets ordered high-to-low.
     */
    public static List<LadderPreset> baselineHls() {
        return BASELINE_HLS;
    }

    public record LadderPreset(int minHeight, int targetWidth, int targetHeight, String suffix) implements Serializable {
    }
}

