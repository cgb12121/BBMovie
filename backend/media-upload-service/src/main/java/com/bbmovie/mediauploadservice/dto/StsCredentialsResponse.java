package com.bbmovie.mediauploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StsCredentialsResponse {
    private String uploadId;
    private String bucket;
    private String objectKey;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String sessionToken;
    private Instant expiration;
    private String region; // Usually "us-east-1" for MinIO
}

