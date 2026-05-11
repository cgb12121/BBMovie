package bbmovie.ai_platform.ai_assests.entity;

public enum AssetStatus {
    /**
     * Presigned URL generated, waiting for physical upload to MinIO.
     */
    UPLOADING,

    /**
     * MinIO confirmed upload via NATS event.
     */
    UPLOADED,

    /**
     * ai-ingestion has started processing the file.
     */
    INGESTING,

    /**
     * Content has been successfully extracted and indexed.
     */
    INGESTED,

    /**
     * User has permanently associated the asset with a chat/entity (Safety from cleanup).
     */
    SAVED,

    /**
     * Processing failed.
     */
    FAILED
}
