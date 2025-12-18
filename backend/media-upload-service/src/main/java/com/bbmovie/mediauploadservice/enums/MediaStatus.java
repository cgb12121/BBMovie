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
     * Processing (Transcoding/Analyzing)
     */
    TRANSCODING,

    /**
     * HLS / MP4 ready
     */
    READY,

    /**
     * Processing failed
     */
    FAILED,
    
    /**
     * Cleaned up due to timeout or manual deletion
     */
    EXPIRED,

    /**
     * Soft deleted
     */
    DELETED
}
