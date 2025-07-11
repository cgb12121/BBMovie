package com.example.bbmovieuploadfile.service.ffmpeg;

@SuppressWarnings({
        "squid:S00115",
        "squid:S1118",
        "unused"
})
public class CodecOptions {
    /**
     * libx264 - Most common software H.264 encoder
     * <p>
     * Strengths:
     * - Great quality output
     * - Widely supported across devices/platforms
     * - Reasonable encoding speed
     * <p>
     * Weaknesses:
     * - Slower than hardware encoders
     * - CPU-intensive encoding
     */
    public static final String libx264 = "libx264";

    /**
     * libx265 - H.265/HEVC software encoder
     * <p>
     * Strengths:
     * - Better compression (50% smaller files)
     * - Excellent for 4K+ video encoding
     * <p>
     * Weaknesses:
     * - 2-3 times slower than x264
     * - Limited support on older devices
     */
    public static final String libx265 = "libx265";

    /**
     * libvpx - VP8/VP9 encoder used for WebM format
     * <p>
     * Strengths:
     * - Open source codec
     * - Good for web video
     * <p>
     * Weaknesses:
     * - Slower than x264 encoding
     * - Less efficient at lower bitrates
     */
    public static final String libvpx = "libvpx";

    /**
     * h264_nvenc - NVIDIA GPU-accelerated H.264 encoder
     * <p>
     * Strengths:
     * - Hardware-accelerated encoding
     * - Very fast encoding speed
     * <p>
     * Weaknesses:
     * - Requires NVIDIA GPU
     * - Needs driver installation and setup
     */
    public static final String h264_nvenc = "h264_nvenc";

    /**
     * h264_qsv - Intel Quick Sync H.264 encoder
     * <p>
     * Strengths:
     * - Hardware accelerated encoding
     * <p>
     * Weaknesses:
     * - Only works with Intel CPUs
     * - Complex setup process
     */
    public static final String h264_qsv = "h264_qsv";

    /**
     * h264_amf - AMD GPU-accelerated H.264 encoder
     * <p>
     * Strengths:
     * - Hardware accelerated encoding
     * <p>
     * Weaknesses:
     * - Requires AMD GPU
     * - Platform/driver dependent
     */
    public static final String h264_amf = "h264_amf";

    /**
     * mpeg4 - Legacy MPEG-4 Part 2 encoder
     * <p>
     * Strengths:
     * - Ultra-compatible with old devices
     * <p>
     * Weaknesses:
     * - Low quality output
     * - Poor compression efficiency
     */
    public static final String mpeg4 = "mpeg4";

    /**
     * copy - Stream copy mode (no re-encoding)
     * <p>
     * Strengths:
     * - No transcoding, just container change
     * - Extremely fast operation
     * <p>
     * Weaknesses:
     * - Only suitable for trimming/remuxing
     */
    public static final String copy = "copy";
}