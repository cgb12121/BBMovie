package com.bbmovie.transcodeworker.service.ffmpeg.option;

/**
 * Constants class containing commonly used FFmpeg video codec options.
 * Provides static final strings representing various video codecs supported by FFmpeg.
 * These constants are used throughout the transcoding process to specify the target codec.
 */
public class CodecOptions {
    /** H.264 codec using the x264 library - Most common and widely supported codec */
    public static final String libx264 = "libx264";
    /** H.265/HEVC codec using the x265 library - More efficient compression than H.264 */
    public static final String libx265 = "libx265";
    /** VP9 codec used primarily for WebM format - Google's open source codec */
    public static final String libvpx = "libvpx";
    /** H.264 codec using NVIDIA NVENC hardware acceleration */
    public static final String h264_nvenc = "h264_nvenc";
    /** H.264 codec using Intel Quick Sync Video hardware acceleration */
    public static final String h264_qsv = "h264_qsv";
    /** H.264 codec using AMD Advanced Media Framework hardware acceleration */
    public static final String h264_amf = "h264_amf";
    /** MPEG-4 Part 2 codec - Older compression standard */
    public static final String mpeg4 = "mpeg4";
    /** Copy codec - No re-encoding, copies the stream directly */
    public static final String copy = "copy";
}
