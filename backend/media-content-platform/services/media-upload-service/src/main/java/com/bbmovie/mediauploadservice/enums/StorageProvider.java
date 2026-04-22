package com.bbmovie.mediauploadservice.enums;

public enum StorageProvider {
    MINIO,
    S3,
    CLOUDINARY,
    LOCAL;

    public boolean isCloud() {
        return this != LOCAL;
    }
}
