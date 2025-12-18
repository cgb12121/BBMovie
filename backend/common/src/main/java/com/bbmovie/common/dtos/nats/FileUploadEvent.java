package com.bbmovie.common.dtos.nats;

import com.bbmovie.common.enums.EntityType;
import com.bbmovie.common.enums.Storage;

import java.time.LocalDateTime;

public class FileUploadEvent {

    private String title;
    private EntityType entityType;
    private Storage storage;
    private String url;
    private String publicId;
    private String quality;
    private String uploadedBy;
    private LocalDateTime timestamp;

    public FileUploadEvent() {}

    public FileUploadEvent(
            String title,
            EntityType entityType,
            Storage storage,
            String url,
            String publicId,
            String quality,
            String uploadedBy,
            LocalDateTime timestamp
    ) {
        this.title = title;
        this.entityType = entityType;
        this.storage = storage;
        this.url = url;
        this.publicId = publicId;
        this.quality = quality;
        this.uploadedBy = uploadedBy;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private EntityType entityType;
        private Storage storage;
        private String url;
        private String publicId;
        private String quality;
        private String uploadedBy;
        private LocalDateTime timestamp;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder entityType(EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder storage(Storage storage) {
            this.storage = storage;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder publicId(String publicId) {
            this.publicId = publicId;
            return this;
        }

        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        public Builder uploadedBy(String uploadedBy) {
            this.uploadedBy = uploadedBy;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public FileUploadEvent build() {
            return new FileUploadEvent(
                    title,
                    entityType,
                    storage,
                    url,
                    publicId,
                    quality,
                    uploadedBy,
                    timestamp
            );
        }
    }

    /* getters & setters (rút gọn nếu muốn) */
}