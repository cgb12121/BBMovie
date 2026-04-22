package com.bbmovie.transcodeworker.service.ffmpeg.option;

/**
 * Constants class containing commonly used FFmpeg encoding preset options.
 * These presets balance encoding speed versus compression efficiency.
 * Slower presets provide better compression (smaller file size for the same quality) but take longer to encode.
 */
public class PresetOptions {
    /** Placebo preset - Very slow, barely any compression improvement over veryslow */
    public static final String placebo = "placebo";
    /** Very slow preset - Better compression efficiency but slower encoding */
    public static final String veryslow = "veryslow";
    /** Slower preset - Good compression efficiency with moderate encoding time */
    public static final String slower = "slower";
    /** Slow preset - Good compression efficiency */
    public static final String slow = "slow";
    /** Medium preset - Balanced between compression and encoding speed */
    public static final String medium = "medium";
    /** Fast preset - Faster encoding with lower compression efficiency */
    public static final String fast = "fast";
    /** Faster preset - Quicker encoding with less compression efficiency */
    public static final String faster = "faster";
    /** Very fast preset - Fast encoding with reduced compression efficiency */
    public static final String veryfast = "veryfast";
    /** Super fast preset - Very fast encoding with low compression efficiency */
    public static final String superfast = "superfast";
    /** Ultra fast preset - Fastest encoding with lowest compression efficiency */
    public static final String ultrafast = "ultrafast";
}
