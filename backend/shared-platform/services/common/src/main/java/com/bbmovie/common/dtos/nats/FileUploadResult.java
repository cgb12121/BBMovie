package com.bbmovie.common.dtos.nats;

public class FileUploadResult {

    private String url;
    private String publicId;
    private String contentType;
    private Long fileSize;

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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public FileUploadResult() {}

    public FileUploadResult(String url, String publicId) {
        this.url = url;
        this.publicId = publicId;
    }

    public FileUploadResult(String url, String publicId, String contentType, Long fileSize) {
        this.url = url;
        this.publicId = publicId;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String publicId;
        private String contentType;
        private Long fileSize;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder publicId(String publicId) {
            this.publicId = publicId;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public FileUploadResult build() {
            return new FileUploadResult(url, publicId, contentType, fileSize);
        }
    }
}
