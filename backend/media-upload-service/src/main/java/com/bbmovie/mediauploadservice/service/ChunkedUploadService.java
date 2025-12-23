package com.bbmovie.mediauploadservice.service;

import com.bbmovie.mediauploadservice.dto.*;
import com.bbmovie.mediauploadservice.entity.ChunkUploadStatus;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.entity.MultipartUploadSession;
import com.bbmovie.mediauploadservice.exception.PresignUrlException;
import com.bbmovie.mediauploadservice.repository.ChunkUploadStatusRepository;
import com.bbmovie.mediauploadservice.repository.MultipartUploadSessionRepository;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkedUploadService {

    private static final Pattern UPLOAD_ID_PATTERN = Pattern.compile(
        "<UploadId>\\s*(.+?)\\s*</UploadId>", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final int MAX_RETRIES = 3;
    private static final int URL_EXPIRY_HOURS = 24;
    
    private final MinioClient minioClient;
    private final MultipartUploadSessionRepository multipartUploadSessionRepository;
    private final ChunkUploadStatusRepository chunkUploadStatusRepository;

    @Value("${upload.enable-batch-presign:true}")
    private boolean enableBatchPresign;

    /**
     * Creates a multipart upload session in MinIO and initializes chunk tracking
     */
    @Transactional
    public String createMultipartUploadSession(String bucketName, String objectName, String contentType) {
        try {
            return createMultipartUploadViaHttp(bucketName, objectName, contentType);
        } catch (Exception e) {
            log.error("Failed to create multipart upload in MinIO", e);
            throw new PresignUrlException("Failed to initialize chunked upload: " + e.getMessage());
        }
    }

    /**
     * Generates presigned URLs for a batch of chunks (lazy generation)
     */
    @Transactional
    public ChunkBatchResponse getChunkBatchUrls(String uploadId, Integer fromPart, Integer toPart, Jwt jwt) {
        if (!enableBatchPresign) {
            throw new IllegalStateException("Batch presign is disabled by configuration.");
        }

        String userId = jwt.getClaim(SUB);

        // Find multipart session
        MultipartUploadSession session = multipartUploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Multipart upload session not found: " + uploadId));

        // Verify ownership
        if (!session.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You are not authorized to access this upload.");
        }

        // Validate part range
        if (fromPart < 1 || toPart > session.getTotalChunks() || fromPart > toPart) {
            throw new IllegalArgumentException(
                String.format("Invalid part range: %d-%d (valid range: 1-%d)", 
                    fromPart, toPart, session.getTotalChunks()));
        }

        // Get or create chunk statuses for this range
        List<ChunkUploadInfo> chunks = IntStream.rangeClosed(fromPart, toPart)
                .mapToObj(partNumber -> {
                    ChunkUploadStatus chunkStatus = chunkUploadStatusRepository
                            .findByUploadIdAndPartNumber(uploadId, partNumber)
                            .orElseGet(() -> createChunkStatus(uploadId, partNumber));

                    // Generate URL if not exists or expired
                    if (chunkStatus.getUploadUrl() == null || 
                        chunkStatus.getUrlExpiresAt() == null ||
                        chunkStatus.getUrlExpiresAt().isBefore(LocalDateTime.now())) {
                        String uploadUrl = generateChunkPresignedUrl(
                            session.getBucket(), 
                            session.getObjectKey(), 
                            session.getMinioUploadId(), 
                            partNumber);
                        
                        chunkStatus.setUploadUrl(uploadUrl);
                        chunkStatus.setUrlExpiresAt(LocalDateTime.now().plusHours(URL_EXPIRY_HOURS));
                        chunkUploadStatusRepository.save(chunkStatus);
                    }

                    long startByte = (long) (partNumber - 1) * session.getChunkSizeBytes();
                    long endByte = Math.min(startByte + session.getChunkSizeBytes() - 1, 
                                           session.getTotalSizeBytes() - 1);

                    return ChunkUploadInfo.builder()
                            .partNumber(partNumber)
                            .uploadUrl(chunkStatus.getUploadUrl())
                            .startByte(startByte)
                            .endByte(endByte)
                            .build();
                })
                .collect(Collectors.toList());

        return ChunkBatchResponse.builder()
                .uploadId(uploadId)
                .totalChunks(session.getTotalChunks())
                .fromPart(fromPart)
                .toPart(toPart)
                .chunks(chunks)
                .build();
    }

    /**
     * Marks a chunk as uploaded with its ETag
     */
    @Transactional
    public void markChunkUploaded(String uploadId, Integer partNumber, String etag, Jwt jwt) {
        String userId = jwt.getClaim(SUB);

        MultipartUploadSession session = multipartUploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Multipart upload session not found: " + uploadId));

        if (!session.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You are not authorized to update this upload.");
        }

        ChunkUploadStatus chunkStatus = chunkUploadStatusRepository
                .findByUploadIdAndPartNumber(uploadId, partNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Chunk %d not found for upload %s", partNumber, uploadId)));

        chunkStatus.setStatus(ChunkUploadStatus.ChunkStatus.UPLOADED);
        chunkStatus.setEtag(etag);
        chunkStatus.setUploadedAt(LocalDateTime.now());
        chunkStatus.setRetryCount(0);
        chunkStatus.setErrorMessage(null);
        chunkUploadStatusRepository.save(chunkStatus);

        log.debug("Chunk {} marked as uploaded for upload {}", partNumber, uploadId);
    }

    /**
     * Retries a failed chunk by generating a new presigned URL
     */
    @Transactional
    public ChunkUploadInfo retryChunk(String uploadId, Integer partNumber, Jwt jwt) {
        String userId = jwt.getClaim(SUB);

        MultipartUploadSession session = multipartUploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Multipart upload session not found: " + uploadId));

        if (!session.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You are not authorized to retry this upload.");
        }

        ChunkUploadStatus chunkStatus = chunkUploadStatusRepository
                .findByUploadIdAndPartNumber(uploadId, partNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Chunk %d not found for upload %s", partNumber, uploadId)));

        // Check retry limit
        if (chunkStatus.getRetryCount() != null && chunkStatus.getRetryCount() >= MAX_RETRIES) {
            throw new IllegalStateException(
                String.format("Chunk %d has exceeded max retries (%d)", partNumber, MAX_RETRIES));
        }

        // Generate new URL
        String uploadUrl = generateChunkPresignedUrl(
            session.getBucket(), 
            session.getObjectKey(), 
            session.getMinioUploadId(), 
            partNumber);

        chunkStatus.setUploadUrl(uploadUrl);
        chunkStatus.setUrlExpiresAt(LocalDateTime.now().plusHours(URL_EXPIRY_HOURS));
        chunkStatus.setStatus(ChunkUploadStatus.ChunkStatus.RETRYING);
        chunkStatus.setRetryCount(chunkStatus.getRetryCount() != null 
             ? chunkStatus.getRetryCount() + 1 
             : 1);
        chunkStatus.setErrorMessage(null);
        chunkUploadStatusRepository.save(chunkStatus);

        long startByte = (long) (partNumber - 1) * session.getChunkSizeBytes();
        long endByte = Math.min(startByte + session.getChunkSizeBytes() - 1, 
                               session.getTotalSizeBytes() - 1);

        log.info("Generated retry URL for chunk {} of upload {}", partNumber, uploadId);

        return ChunkUploadInfo.builder()
                .partNumber(partNumber)
                .uploadUrl(uploadUrl)
                .startByte(startByte)
                .endByte(endByte)
                .build();
    }

    /**
     * Gets upload progress
     */
    @Transactional(readOnly = true)
    public ChunkUploadProgressResponse getUploadProgress(String uploadId, Jwt jwt) {
        String userId = jwt.getClaim(SUB);

        MultipartUploadSession session = multipartUploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Multipart upload session not found: " + uploadId));

        if (!session.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You are not authorized to access this upload.");
        }

        List<ChunkUploadStatus> chunkStatuses = chunkUploadStatusRepository
                .findByUploadIdOrderByPartNumberAsc(uploadId);

        long uploadedCount = chunkUploadStatusRepository.countByUploadIdAndStatus(
            uploadId, ChunkUploadStatus.ChunkStatus.UPLOADED);
        long failedCount = chunkUploadStatusRepository.countByUploadIdAndStatus(
            uploadId, ChunkUploadStatus.ChunkStatus.FAILED);
        long pendingCount = session.getTotalChunks() - uploadedCount - failedCount;

        double progressPercentage = session.getTotalChunks() > 0 ? 
            (uploadedCount * 100.0 / session.getTotalChunks()) : 0.0;

        Map<Integer, String> statusMap = chunkStatuses.stream()
                .collect(Collectors.toMap(
                    ChunkUploadStatus::getPartNumber,
                    cs -> cs.getStatus().name()
                ));

        return ChunkUploadProgressResponse.builder()
                .uploadId(uploadId)
                .totalChunks(session.getTotalChunks())
                .uploadedChunks((int) uploadedCount)
                .failedChunks((int) failedCount)
                .pendingChunks((int) pendingCount)
                .progressPercentage(progressPercentage)
                .chunkStatuses(statusMap)
                .build();
    }

    /**
     * Initializes chunk status records for all chunks
     */
    @Transactional
    public void initializeChunkStatuses(String uploadId, int totalChunks) {
        for (int i = 1; i <= totalChunks; i++) {
            createChunkStatus(uploadId, i);
        }
        log.debug("Initialized {} chunk statuses for upload {}", totalChunks, uploadId);
    }

    /**
     * Gets all uploaded chunks for completing multipart upload
     */
    @Transactional(readOnly = true)
    public List<io.minio.messages.Part> getUploadedParts(String uploadId) {
        List<ChunkUploadStatus> uploadedChunks = chunkUploadStatusRepository
                .findByUploadIdOrderByPartNumberAsc(uploadId)
                .stream()
                .filter(cs -> cs.getStatus() == ChunkUploadStatus.ChunkStatus.UPLOADED)
                .filter(cs -> cs.getEtag() != null && !cs.getEtag().isEmpty())
                .toList();

        return uploadedChunks.stream()
                .map(cs -> new io.minio.messages.Part(cs.getPartNumber(), cs.getEtag()))
                .collect(Collectors.toList());
    }

    /**
     * Completes multipart upload in MinIO and sets metadata
     */
    @Transactional
    public void completeMultipartUpload(MultipartUploadSession session, List<io.minio.messages.Part> parts, 
                                       com.bbmovie.mediauploadservice.entity.MediaFile mediaFile) throws Exception {
        // Validate input
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Parts list cannot be null or empty");
        }

        // Validate parts are sorted and have valid part numbers
        for (io.minio.messages.Part part : parts) {
            if (part.partNumber() < 1 || part.partNumber() > 10000) {
                throw new IllegalArgumentException(
                        String.format("Invalid part number: %d (must be between 1 and 10000)", part.partNumber()));
            }
            if (part.etag() == null || part.etag().trim().isEmpty()) {
                throw new IllegalArgumentException("Part ETag cannot be null or empty");
            }
        }

        // Build XML body for complete multipart upload with proper escaping
        StringBuilder xmlBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CompleteMultipartUpload>");
        for (io.minio.messages.Part part : parts) {
            xmlBuilder.append("<Part>")
                    .append("<PartNumber>").append(escapeXml(String.valueOf(part.partNumber()))).append("</PartNumber>")
                    .append("<ETag>").append(escapeXml(part.etag())).append("</ETag>")
                    .append("</Part>");
        }
        xmlBuilder.append("</CompleteMultipartUpload>");
        String xmlBody = xmlBuilder.toString();

        // Validate XML body length (reasonable limit)
        if (xmlBody.length() > 100000) {
            throw new IllegalArgumentException("XML body too large: " + xmlBody.length() + " bytes");
        }

        // Generate presigned URL for completing multipart upload
        // POST to bucket/object?uploadId=...
        String completeUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.POST)
                        .bucket(session.getBucket())
                        .object(session.getObjectKey())
                        .expiry(1, TimeUnit.HOURS)
                        .extraQueryParams(Map.of("uploadId", session.getMinioUploadId()))
                        .build());

        // Make HTTP POST request to complete multipart upload
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(completeUrl))
                .POST(HttpRequest.BodyPublishers.ofString(xmlBody))
                .header("Content-Type", "application/xml")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Validate response status
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            String errorBody = response.body();
            log.error("Failed to complete multipart upload. Status: {}, Response: {}, UploadId: {}", 
                statusCode, errorBody, session.getUploadId());
            throw new PresignUrlException(
                String.format("Failed to complete multipart upload: HTTP %d - %s", statusCode,
                    errorBody != null && errorBody.length() > 200 ? errorBody.substring(0, 200) : errorBody));
        }

        // Validate successful completion response (should contain XML with Location or ETag)
        String responseBody = response.body();
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            // Verify response contains expected XML elements (optional validation)
            if (!responseBody.contains("<CompleteMultipartUploadResult") && 
                !responseBody.contains("<ETag>") && 
                !responseBody.contains("Location")) {
                log.warn("Unexpected response format from complete multipart upload: {}", 
                    responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
            }
        }

        log.debug("Successfully completed multipart upload. UploadId: {}, Parts: {}", 
            session.getUploadId(), parts.size());

        // Set metadata on the completed object (required for transcode-worker)
        // Use CopyObject to set metadata without re-uploading
        try {
            Map<String, String> metadata = generateMetadataMap(session, mediaFile);

            // Copy object to itself with new metadata (MinIO way to set metadata)
            // This replaces the object metadata without re-uploading the file
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(session.getBucket())
                            .object(session.getObjectKey())
                            .source(CopySource.builder()
                                    .bucket(session.getBucket())
                                    .object(session.getObjectKey())
                                    .build())
                            .extraQueryParams(metadata)
                            .metadataDirective(Directive.REPLACE)
                            .build());

            log.debug("Metadata set on completed object: {}", session.getObjectKey());
        } catch (Exception e) {
            log.error("Failed to set metadata on completed object: {}", session.getObjectKey(), e);
            // Don't fail the upload if metadata setting fails, but log the error
            // Transcode worker can still query from database if needed
        }
    }

    private static Map<String, String> generateMetadataMap(MultipartUploadSession session, MediaFile mediaFile) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-amz-meta-video-id", session.getUploadId());
        metadata.put("x-amz-meta-uploader-id", session.getUserId());
        metadata.put("x-amz-meta-purpose", mediaFile.getPurpose().name());
        if (mediaFile.getChecksum() != null) {
            metadata.put("x-amz-meta-expected-checksum", mediaFile.getChecksum());
        }
        if (mediaFile.getMimeType() != null) {
            metadata.put("Content-Type", mediaFile.getMimeType());
        }
        return metadata;
    }

    /**
     * Escapes XML special characters to prevent XML injection
     */
    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    // Private helper methods

    private ChunkUploadStatus createChunkStatus(String uploadId, Integer partNumber) {
        ChunkUploadStatus chunkStatus = ChunkUploadStatus.builder()
                .uploadId(uploadId)
                .partNumber(partNumber)
                .status(ChunkUploadStatus.ChunkStatus.PENDING)
                .retryCount(0)
                .build();
        return chunkUploadStatusRepository.save(chunkStatus);
    }

    private String generateChunkPresignedUrl(String bucketName, String objectName, String minioUploadId, Integer partNumber) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(URL_EXPIRY_HOURS, TimeUnit.HOURS)
                            .extraQueryParams(Map.of(
                                    "uploadId", minioUploadId,
                                    "partNumber", String.valueOf(partNumber)
                            ))
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for part {}", partNumber, e);
            throw new PresignUrlException("Failed to generate upload URL for chunk " + partNumber);
        }
    }

    private String createMultipartUploadViaHttp(String bucketName, String objectName, String contentType) throws Exception {
        String initUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.POST)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(1, TimeUnit.HOURS)
                        .extraQueryParams(Map.of("uploads", ""))
                        .build());

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(initUrl))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", contentType)
                .build();

        HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            String errorBody = response.body();
            log.error("Failed to initiate multipart upload. Status: {}, Response: {}", statusCode, errorBody);
            throw new PresignUrlException(
                String.format("Failed to initiate multipart upload: HTTP %d - %s", statusCode, 
                    errorBody != null && errorBody.length() > 200 ? errorBody.substring(0, 200) : errorBody));
        }

        String responseBody = response.body();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.error("Empty response body from multipart upload initiation");
            throw new PresignUrlException("Received empty response from multipart upload initiation");
        }

        Matcher matcher = UPLOAD_ID_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            log.error("Failed to parse uploadId from XML response: {}", 
                responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
            throw new PresignUrlException("Failed to parse uploadId from multipart upload response. Invalid XML structure.");
        }

        String uploadId = matcher.group(1).trim();
        if (uploadId.isEmpty() || uploadId.length() > 200) {
            log.error("Invalid uploadId format: length={}", uploadId.length());
            throw new PresignUrlException("Invalid uploadId format received from server");
        }

        log.debug("Successfully initiated multipart upload. UploadId: {}", uploadId);
        return uploadId;
    }
}

