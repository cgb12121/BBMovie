-- V1: Netflix-style analysis tables (MySQL/H2 compatible)

CREATE TABLE tb_complexity_profile (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    upload_id VARCHAR(128) NOT NULL,
    analysis_version INT NOT NULL,
    content_class VARCHAR(64),
    complexity_score DOUBLE NOT NULL,
    feature_scores_json VARCHAR(16384),
    recipe_hints_json VARCHAR(8192),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_complexity_upload_version UNIQUE (upload_id, analysis_version)
);

CREATE TABLE tb_validation_report (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    upload_id VARCHAR(128) NOT NULL,
    rendition_suffix VARCHAR(64) NOT NULL,
    analysis_version INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    violations_json VARCHAR(16384),
    ffprobe_artifact_uri VARCHAR(1024),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_validation_upload_rendition_version UNIQUE (upload_id, rendition_suffix, analysis_version)
);

CREATE TABLE tb_quality_report (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    upload_id VARCHAR(128) NOT NULL,
    rendition_suffix VARCHAR(64) NOT NULL,
    analysis_version INT NOT NULL,
    metric VARCHAR(32) NOT NULL,
    score DOUBLE NOT NULL,
    psnr_db DOUBLE,
    ssim_score DOUBLE,
    artifact_uri VARCHAR(1024),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_quality_upload_rendition_version UNIQUE (upload_id, rendition_suffix, analysis_version)
);

CREATE TABLE tb_analysis_job (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    upload_id VARCHAR(128) NOT NULL,
    job_kind VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_complexity_upload ON tb_complexity_profile (upload_id);
CREATE INDEX idx_validation_upload ON tb_validation_report (upload_id);
CREATE INDEX idx_quality_upload ON tb_quality_report (upload_id);
CREATE INDEX idx_analysis_job_upload ON tb_analysis_job (upload_id);
