package com.bbmovie.transcodeworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tb_quality_report")
public class QualityReportEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "upload_id", nullable = false, length = 128)
    private String uploadId;

    @Column(name = "rendition_suffix", nullable = false, length = 64)
    private String renditionSuffix;

    @Column(name = "analysis_version", nullable = false)
    private int analysisVersion;

    @Column(name = "metric", nullable = false, length = 32)
    private String metric;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "psnr_db")
    private Double psnrDb;

    @Column(name = "ssim_score")
    private Double ssimScore;

    @Column(name = "artifact_uri", length = 1024)
    private String artifactUri;

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

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Double getPsnrDb() {
        return psnrDb;
    }

    public void setPsnrDb(Double psnrDb) {
        this.psnrDb = psnrDb;
    }

    public Double getSsimScore() {
        return ssimScore;
    }

    public void setSsimScore(Double ssimScore) {
        this.ssimScore = ssimScore;
    }

    public String getArtifactUri() {
        return artifactUri;
    }

    public void setArtifactUri(String artifactUri) {
        this.artifactUri = artifactUri;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
