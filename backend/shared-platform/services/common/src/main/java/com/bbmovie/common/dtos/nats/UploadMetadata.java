package com.bbmovie.common.dtos.nats;

import com.bbmovie.common.enums.EntityType;
import com.bbmovie.common.enums.Storage;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public class UploadMetadata {

    @NotNull
    private String fileId;

    @NotNull
    private EntityType fileType;

    @NotNull
    private Storage storage;

    @Nullable
    private String quality;

    public UploadMetadata() {}

    public UploadMetadata(String fileId, EntityType fileType, Storage storage, String quality) {
        this.fileId = fileId;
        this.fileType = fileType;
        this.storage = storage;
        this.quality = quality;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public EntityType getFileType() {
        return fileType;
    }

    public void setFileType(EntityType fileType) {
        this.fileType = fileType;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    @Nullable
    public String getQuality() {
        return quality;
    }

    public void setQuality(@Nullable String quality) {
        this.quality = quality;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fileId;
        private EntityType fileType;
        private Storage storage;
        private String quality;

        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public Builder fileType(EntityType fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder storage(Storage storage) {
            this.storage = storage;
            return this;
        }

        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        public UploadMetadata build() {
            return new UploadMetadata(fileId, fileType, storage, quality);
        }
    }
}
