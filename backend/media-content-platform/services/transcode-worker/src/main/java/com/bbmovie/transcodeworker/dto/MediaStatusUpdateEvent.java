package com.bbmovie.transcodeworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing a media status update event.
 * Used to communicate status changes for media processing operations through the messaging system.
 * This class is marked with Lombok annotations to automatically generate boilerplate code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaStatusUpdateEvent {

    /** Unique identifier for the upload operation */
    private String uploadId;

    /** Current status of the media processing operation (e.g., PROCESSING, COMPLETED, FAILED) */
    private String status; // Using String to avoid Enum dependency coupling, or duplicate Enum

    /** Reason for the status change, particularly useful for ERROR status */
    private String reason;

    /** Checksum of the processed media file */
    private String checksum;

    /** Size of the processed media file in bytes */
    private Long fileSize;

    /** Sparse checksum for integrity verification */
    private String sparseChecksum;

    /** Duration of the video in seconds (extracted from metadata) */
    private Double duration;

    /** Path to HLS master playlist (e.g., movies/{uploadId}/master.m3u8) */
    private String filePath;

    /** Available transcoded resolutions for this media (e.g., ["360p","720p","1080p"]) */
    private List<String> availableResolutions;

    /** Highest available transcoded resolution (e.g., "1080p", "2160p") */
    private String maxAvailableResolution;

    /** Source video width from probe/transcode metadata */
    private Integer sourceWidth;

    /** Source video height from probe/transcode metadata */
    private Integer sourceHeight;
}
