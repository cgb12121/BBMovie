package com.example.bbmovieuploadfile.service.ffmpeg;

@SuppressWarnings({
        "squid:S00115",
        "squid:S1118",
        "unused"
})public class PresetOptions {
    /**
     * placebo - Slowest preset with the smallest file size and best encoding quality
     * <p>
     * Characteristics:
     * - Speed: 🐢 Slowest
     * - File Size: 📉 Smallest
     * - Quality: 🟢 Best (insignificant over veryslow)
     */
    public static final String placebo = "placebo";

    /**
     * veryslow - Very slow preset with high-quality output
     * <p>
     * Characteristics:
     * - Speed: 🐌 Very slow
     * - File Size: Small
     * - Quality: Best (practical maximum)
     */
    public static final String veryslow = "veryslow";

    /**
     * slower - Slow preset with very good quality
     * <p>
     * Characteristics:
     * - Speed: Slow
     * - File Size: Medium
     * - Quality: Very good
     */
    public static final String slower = "slower";

    /**
     * slow - Balanced preset with good quality
     * <p>
     * Characteristics:
     * - Speed: Balanced
     * - File Size: Medium
     * - Quality: Good
     */
    public static final String slow = "slow";

    /**
     * medium - Default balanced preset
     * <p>
     * Characteristics:
     * - Speed: Balanced
     * - File Size: Standard
     * - Quality: Standard
     */
    public static final String medium = "medium";

    /**
     * fast - Fast preset with slightly worse quality
     * <p>
     * Characteristics:
     * - Speed: Fast
     * - File Size: Larger
     * - Quality: Slightly worse
     */
    public static final String fast = "fast";

    /**
     * faster - Faster preset with lower quality
     * <p>
     * Characteristics:
     * - Speed: Faster
     * - File Size: Bigger
     * - Quality: Lower
     */
    public static final String faster = "faster";

    /**
     * veryfast - Very fast preset with lower quality
     * <p>
     * Characteristics:
     * - Speed: ⚡ Very fast
     * - File Size: 📈 Larger
     * - Quality: Lower
     */
    public static final String veryfast = "veryfast";

    /**
     * superfast - Super fast preset with lower quality
     * <p>
     * Characteristics:
     * - Speed: ⚡⚡ Super fast
     * - File Size: 📈📈 Bigger
     * - Quality: Lower
     */
    public static final String superfast = "superfast";

    /**
     * ultrafast - Fastest preset with the worst quality
     * <p>
     * Characteristics:
     * - Speed: 🚀 Fastest
     * - File Size: 📈📈📈 Biggest
     * - Quality: Worst (but fast!)
     */
    public static final String ultrafast = "ultrafast";
}
