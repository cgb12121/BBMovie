package com.bbmovie.transcodeworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tb_complexity_profile")
public class ComplexityProfileEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "upload_id", nullable = false, length = 128)
    private String uploadId;

    @Column(name = "analysis_version", nullable = false)
    private int analysisVersion;

    @Column(name = "content_class", length = 64)
    private String contentClass;

    @Column(name = "complexity_score", nullable = false)
    private double complexityScore;

    @Column(name = "feature_scores_json", length = 16384)
    private String featureScoresJson;

    @Column(name = "recipe_hints_json", length = 8192)
    private String recipeHintsJson;

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

    public int getAnalysisVersion() {
        return analysisVersion;
    }

    public void setAnalysisVersion(int analysisVersion) {
        this.analysisVersion = analysisVersion;
    }

    public String getContentClass() {
        return contentClass;
    }

    public void setContentClass(String contentClass) {
        this.contentClass = contentClass;
    }

    public double getComplexityScore() {
        return complexityScore;
    }

    public void setComplexityScore(double complexityScore) {
        this.complexityScore = complexityScore;
    }

    public String getFeatureScoresJson() {
        return featureScoresJson;
    }

    public void setFeatureScoresJson(String featureScoresJson) {
        this.featureScoresJson = featureScoresJson;
    }

    public String getRecipeHintsJson() {
        return recipeHintsJson;
    }

    public void setRecipeHintsJson(String recipeHintsJson) {
        this.recipeHintsJson = recipeHintsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
