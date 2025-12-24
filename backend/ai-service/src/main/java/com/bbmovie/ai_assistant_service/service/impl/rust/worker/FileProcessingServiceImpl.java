package com.bbmovie.ai_assistant_service.service.impl.rust.worker;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto.FileAttachment;
import com.bbmovie.ai_assistant_service.service.*;
import com.bbmovie.ai_assistant_service.service.impl.rust.worker.RustAiContextRefineryClient.RustProcessRequest;
import com.bbmovie.ai_assistant_service.utils.FileTypeUtils;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileProcessingServiceImpl implements FileProcessingService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(FileProcessingServiceImpl.class);

    private final MediaUploadServiceClient mediaUploadServiceClient;
    private final RustAiContextRefineryClient rustClient;

    @Override
    public Mono<FileProcessingResult> processAttachments(List<FileAttachment> attachments, Jwt jwt) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.just(FileProcessingResult.empty());
        }

        log.debug("Processing {} attachments from client", attachments.size());

        // 1. Get download URLs for all files (if not already provided)
        // For files with uploadId but no fileUrl, fetch URL from media-upload-service
        Mono<List<FileAttachment>> enrichedAttachments = Flux.fromIterable(attachments)
                .flatMap(att -> {
                    // If fileUrl is already provided, use it directly
                    if (att.getFileUrl() != null && !att.getFileUrl().isEmpty()) {
                        return Mono.just(att);
                    }
                    
                    // If uploadId is provided, fetch URL from media-upload-service
                    final String uploadId;
                    String tempUploadId = att.getUploadId();
                    if (tempUploadId == null || tempUploadId.isEmpty()) {
                        // Fallback to legacy fileId for backward compatibility
                        if (att.getFileId() != null) {
                            log.warn("Using deprecated fileId field. Please migrate to uploadId.");
                            uploadId = String.valueOf(att.getFileId());
                        } else {
                            return Mono.error(new IllegalArgumentException("FileAttachment must have either uploadId or fileUrl"));
                        }
                    } else {
                        uploadId = tempUploadId;
                    }
                    
                    if (jwt == null) {
                        return Mono.error(new IllegalStateException("JWT token required to get file download URL from media-upload-service"));
                    }
                    
                    String jwtToken = jwt.getTokenValue();
                    return mediaUploadServiceClient.getDownloadUrl(uploadId, jwtToken)
                            .map(fileUrl -> {
                                att.setFileUrl(fileUrl);
                                return att;
                            })
                            .doOnError(e -> log.error("Failed to get download URL for uploadId {}: {}", 
                                    uploadId, e.getMessage()));
                })
                .collectList();

        // 2. Prepare Batch Request for Rust (after getting URLs)
        Mono<List<RustProcessRequest>> rustRequestsMono = enrichedAttachments
                .map(atts -> atts.stream()
                .filter(att -> isSupportedByRust(att.getFilename()))
                .map(att -> {
                    // Get uploadId for status updates
                    String uploadId = att.getUploadId();
                    if ((uploadId == null || uploadId.isEmpty()) && att.getFileId() != null) {
                        uploadId = String.valueOf(att.getFileId());
                    }
                    return new RustProcessRequest(att.getFileUrl(), att.getFilename(), uploadId);
                })
                        .toList());

        // 3. Process Batch via Rust
        Mono<Map<String, String>> processingTask = rustRequestsMono
                .flatMap(rustClient::processBatch)
                .map(results -> results.stream()
                        .filter(node -> node.has("filename") && (node.has("result") || node.has("error")))
                        .collect(Collectors.toMap(
                                node -> node.get("filename").asText(),
                                this::extractResultText
                        )))
                .onErrorResume(e -> {
                    log.error("Batch processing failed: {}", e.getMessage());
                    return Mono.just(Map.of());
                });

        // 4. Combine Results
        return enrichedAttachments
                .zipWith(processingTask)
                .map(tuple -> {
                    List<FileAttachment> finalAttachments = tuple.getT1();
                    Map<String, String> resultsMap = tuple.getT2();
                    
                    List<ProcessedFileContent> processedFiles = finalAttachments.stream()
                            .map(att -> {
                                String extractedText = resultsMap.getOrDefault(att.getFilename(), "");
                                return new ProcessedFileContent(
                                        att.getFilename(),
                                        FileTypeUtils.getExtension(att.getFilename()),
                                        att.getFileUrl(),
                                        extractedText
                                );
                            })
                            .toList();

                    // Build FileReferences list (legacy support)
                    List<String> fileRefs = processedFiles.stream()
                            .map(ProcessedFileContent::url)
                            .toList();
                    
                    List<FileUploadResult> synthesizedUploads = finalAttachments.stream()
                            .map(att -> new FileUploadResult(att.getFileUrl(), att.getFilename()))
                            .toList();

                    return new FileProcessingResult(synthesizedUploads, processedFiles, fileRefs);
                });
    }

    private String extractResultText(JsonNode node) {
        // FIX: Check error at Item level first - Handle error case first
        if (node.has("error") && !node.get("error").isNull()) {
            String errorMsg = node.get("error").asText();
            log.warn("File {} failed in Rust: {}", node.get("filename"), errorMsg);
            // Return error message so AI knows about the failure
            return "[SYSTEM ERROR: Processing failed for this file: " + errorMsg + "]";
        }

        // If no error, then try to get result
        JsonNode result = node.get("result");
        if (result == null) return "[NO RESULT: File processing returned no content]"; // Fallback

        if (result.has("text")) {
            return result.get("text").asText();
        } else if (result.has("ocr_text") || result.has("vision_description")) {
            // Composite result (Image)
            String ocr = result.has("ocr_text") ? result.get("ocr_text").asText() : "";
            String vision = result.has("vision_description") ? result.get("vision_description").asText() : "";

            StringBuilder sb = new StringBuilder();
            if (!ocr.isEmpty()) sb.append("OCR: ").append(ocr).append("\n");
            if (!vision.isEmpty()) sb.append("Vision: ").append(vision);
            return sb.toString().trim();
        } else if (result.has("description")) {
             return result.get("description").asText();
        }

        return result.toString(); // Fallback
    }

    private boolean isSupportedByRust(String filename) {
        String lower = filename.toLowerCase();
        return FileTypeUtils.isImageFile(lower) || 
               FileTypeUtils.isAudioFile(lower) || 
               lower.endsWith(".pdf") || 
               FileTypeUtils.isTextFile(lower);
    }
}
