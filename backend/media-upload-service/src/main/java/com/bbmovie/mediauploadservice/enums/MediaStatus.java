package com.bbmovie.mediauploadservice.enums;

public enum MediaStatus {

    /**
     * Presign URL generated
     * Client has NOT finished uploading
     */
    INITIATED,

    /**
     * MinIO reported CompleteMultipartUpload / PutObject
     */
    UPLOADED,

    /**
     * Tika + ClamAV OK
     */
    VALIDATED,

    /**
     * File has virus / wrong format
     */
    REJECTED,

    /**
     * Processing started (sent by transcode-worker)
     */
    PROCESSING,

    /**
     * Processing (Transcoding/Analyzing) - legacy, use PROCESSING
     */
    TRANSCODING,

    /**
     * Processing completed successfully (sent by transcode-worker)
     */
    COMPLETED,

    /**
     * HLS / MP4 ready - legacy, use COMPLETED
     */
    READY,

    /**
     * Processing failed
     */
    FAILED,

    /**
     * Malware detected in file
     */
    MALWARE_DETECTED,

    /**
     * Invalid file format/content
     */
    INVALID_FILE,
    
    /**
     * Cleaned up due to timeout or manual deletion
     */
    EXPIRED,

    /**
     * Soft deleted
     */
    DELETED
}
