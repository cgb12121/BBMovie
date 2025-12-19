package com.bbmovie.transcodeworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
