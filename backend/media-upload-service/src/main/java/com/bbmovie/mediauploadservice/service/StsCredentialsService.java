package com.bbmovie.mediauploadservice.service;

import com.bbmovie.mediauploadservice.dto.StsCredentialsRequest;
import com.bbmovie.mediauploadservice.dto.StsCredentialsResponse;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.enums.StorageProvider;
import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import com.bbmovie.mediauploadservice.exception.PresignUrlException;
import com.bbmovie.mediauploadservice.repository.MediaFileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.admin.AddServiceAccountResp;
import io.minio.admin.MinioAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.lang.reflect.Method;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class StsCredentialsService {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket.movie-raw:bbmovie-raw}")
    private String rawBucketName;

    @Value("${minio.bucket.ai-assets:bbmovie-ai-assets}")
    private String aiAssetsBucketName;

    @Value("${minio.access-key}")
    private String adminAccessKey;

    @Value("${minio.secret-key}")
    private String adminSecretKey;

    @Value("${upload.enable-sts:true}")
    private boolean enableSts;

    @Value("${upload.sts.use-admin-service-account:false}")
    private boolean useAdminServiceAccount;

    @Value("${minio.admin.url:${minio.url}}")
    private String minioAdminUrl;

    @Value("${minio.admin.access-key:${minio.access-key}}")
    private String minioAdminAccessKey;

    @Value("${minio.admin.secret-key:${minio.secret-key}}")
    private String minioAdminSecretKey;

    @Value("${upload.sts.service-account.expiry-seconds:3600}")
    private int serviceAccountExpirySeconds;

    private final MediaFileRepository mediaFileRepository;
    private final ObjectKeyStrategy objectKeyStrategy;

    /**
     * Generates temporary credentials (STS-style) for direct upload
     * For MinIO, we return admin credentials with restricted policy
     * Frontend can use MinIO SDK with these credentials
     * <p>
     * Note: In production, you should use MinIO's temporary credential service
     * or create temporary users via Admin API
     */
    @Transactional
    public StsCredentialsResponse generateTemporaryCredentials(StsCredentialsRequest request, Jwt jwt) {
        if (!enableSts) {
            throw new AccessDeniedException("STS upload is disabled by configuration.");
        }

        String role = jwt.getClaim(ROLE);
        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException("STS upload requires ADMIN role.");
        }

        String userId = jwt.getClaim(SUB);
        String uploadId = UUID.randomUUID().toString();
        
        // Determine bucket and generate object key
        String bucketName = determineBucket(request.getPurpose());
        String objectKey = objectKeyStrategy.build(request.getPurpose(), uploadId, request.getFilename());
        
        // Register media file
        MediaFile mediaFile = MediaFile.builder()
                .uploadId(uploadId)
                .userId(userId)
                .originalFilename(request.getFilename())
                .bucket(bucketName)
                .objectKey(objectKey)
                .status(MediaStatus.INITIATED)
                .purpose(request.getPurpose())
                .storageProvider(StorageProvider.MINIO)
                .mimeType(request.getContentType())
                .sizeBytes(request.getSizeBytes())
                .build();
        mediaFileRepository.save(mediaFile);

        Instant expiration = Instant.now().plus(request.getDurationSeconds(), ChronoUnit.SECONDS);

        String policyJson = createUploadPolicy(bucketName, objectKey);

        if (useAdminServiceAccount) {
            ServiceAccountCredentials sac = createServiceAccount(policyJson, serviceAccountExpirySeconds);
            log.info("Generated service account credentials for upload: {} (user: {})", uploadId, userId);
            return StsCredentialsResponse.builder()
                    .uploadId(uploadId)
                    .bucket(bucketName)
                    .objectKey(objectKey)
                    .endpoint(minioUrl)
                    .accessKey(sac.accessKey())
                    .secretKey(sac.secretKey())
                    .sessionToken(null)
                    .expiration(expiration)
                    .region("us-east-1")
                    .build();
        } else {
            log.warn("Using admin credentials for STS response (policy embedded). Consider enabling service-account mode.");
            return StsCredentialsResponse.builder()
                    .uploadId(uploadId)
                    .bucket(bucketName)
                    .objectKey(objectKey)
                    .endpoint(minioUrl)
                    .accessKey(adminAccessKey) // In production, use temporary credentials
                    .secretKey(adminSecretKey)  // In production, use temporary credentials
                    .sessionToken(policyJson)  // Policy JSON for frontend validation
                    .expiration(expiration)
                    .region("us-east-1") // MinIO default region
                    .build();
        }
    }

    /**
     * Creates IAM policy JSON that restricts upload to a specific bucket and object
     */
    private String createUploadPolicy(String bucketName, String objectKey) {
        // Create a policy that allows PutObject only to the specific object
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("Version", "2012-10-17");
        
        List<Map<String, Object>> statements = new ArrayList<>();
        
        // Statement 1: Allow PutObject to specific object
        Map<String, Object> statement1 = new LinkedHashMap<>();
        statement1.put("Effect", "Allow");
        statement1.put("Action", List.of("s3:PutObject"));
        statement1.put("Resource", List.of("arn:aws:s3:::" + bucketName + "/" + objectKey));
        statements.add(statement1);
        
        // Statement 2: Allow multipart upload operations
        Map<String, Object> statement2 = new LinkedHashMap<>();
        statement2.put("Effect", "Allow");
        statement2.put("Action", Arrays.asList(
            "s3:CreateMultipartUpload",
            "s3:UploadPart",
            "s3:CompleteMultipartUpload",
            "s3:AbortMultipartUpload"
        ));
        statement2.put("Resource", List.of("arn:aws:s3:::" + bucketName + "/" + objectKey));
        statements.add(statement2);
        
        policy.put("Statement", statements);
        
        try {
            // Convert to JSON string
            return new ObjectMapper().writeValueAsString(policy);
        } catch (Exception e) {
            log.error("Failed to create policy JSON", e);
            throw new PresignUrlException("Failed to generate policy");
        }
    }

    private String determineBucket(UploadPurpose purpose) {
        return switch (purpose) {
            case AI_ASSET -> aiAssetsBucketName;
            case USER_AVATAR, MOVIE_POSTER, MOVIE_TRAILER, MOVIE_SOURCE -> rawBucketName;
        };
    }

    private ServiceAccountCredentials createServiceAccount(String policyJson, int expirySeconds) {
        try {
            MinioAdminClient adminClient = MinioAdminClient.builder()
                    .endpoint(minioAdminUrl)
                    .credentials(minioAdminAccessKey, minioAdminSecretKey)
                    .build();

            AddServiceAccountResp resp = tryAddServiceAccount(adminClient, policyJson, expirySeconds);
            if (resp == null) {
                throw new PresignUrlException("Failed to create service account via MinIO Admin API");
            }
            String access = firstNonNull(
                    invokeString(resp, "accessKey"),
                    invokeString(resp, "getAccessKey"));
            String secret = firstNonNull(
                    invokeString(resp, "secretKey"),
                    invokeString(resp, "getSecretKey"));
            if (access == null || secret == null) {
                throw new PresignUrlException("Service account response missing keys");
            }
            return new ServiceAccountCredentials(access, secret);
        } catch (Exception e) {
            log.error("Failed to create service account via MinIO Admin API", e);
            throw new PresignUrlException("Failed to create temporary credentials: " + e.getMessage());
        }
    }

    private AddServiceAccountResp tryAddServiceAccount(MinioAdminClient adminClient, String policyJson, int expirySeconds) {
        // Attempt common method signatures reflectively to stay compatible
        for (Method m : MinioAdminClient.class.getMethods()) {
            if (!m.getName().equals("addServiceAccount")) continue;
            Class<?>[] params = m.getParameterTypes();
            try {
                if (params.length == 2 && params[0] == String.class && (params[1] == Integer.class || params[1] == int.class)) {
                    Object resp = m.invoke(adminClient, policyJson, expirySeconds);
                    if (resp instanceof AddServiceAccountResp r) return r;
                } else if (params.length == 1 && params[0] == String.class) {
                    Object resp = m.invoke(adminClient, policyJson);
                    if (resp instanceof AddServiceAccountResp r) return r;
                }
            } catch (Exception e) {
                log.warn("addServiceAccount invocation failed with signature {}: {}", Arrays.toString(params), e.getMessage());
            }
        }
        return null;
    }

    private String invokeString(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v != null ? v.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonNull(String... vals) {
        for (String v : vals) {
            if (v != null) return v;
        }
        return null;
    }

    private record ServiceAccountCredentials(String accessKey, String secretKey) {}
}

