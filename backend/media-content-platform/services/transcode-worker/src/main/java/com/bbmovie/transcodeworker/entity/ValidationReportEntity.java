package com.bbmovie.transcodeworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tb_validation_report")
public class ValidationReportEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "upload_id", nullable = false, length = 128)
    private String uploadId;

    @Column(name = "rendition_suffix", nullable = false, length = 64)
    private String renditionSuffix;

    @Column(name = "analysis_version", nullable = false)
    private int analysisVersion;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "violations_json", length = 16384)
    private String violationsJson;

    @Column(name = "ffprobe_artifact_uri", length = 1024)
    private String ffprobeArtifactUri;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getRenditionSuffix() {
        return renditionSuffix;
    }

    public void setRenditionSuffix(String renditionSuffix) {
        this.renditionSuffix = renditionSuffix;
    }

    public int getAnalysisVersion() {
        return analysisVersion;
    }

    public void setAnalysisVersion(int analysisVersion) {
        this.analysisVersion = analysisVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getViolationsJson() {
        return violationsJson;
    }

    public void setViolationsJson(String violationsJson) {
        this.violationsJson = violationsJson;
    }

    public String getFfprobeArtifactUri() {
        return ffprobeArtifactUri;
    }

    public void setFfprobeArtifactUri(String ffprobeArtifactUri) {
        this.ffprobeArtifactUri = ffprobeArtifactUri;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
