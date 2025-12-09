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

    private final FileUploadClient fileUploadClient;
    private final RustAiContextRefineryClient rustClient;

    @Override
    public Mono<FileProcessingResult> processAttachments(List<FileAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.just(FileProcessingResult.empty());
        }

        log.info("Processing {} attachments from client", attachments.size());

        // 1. Confirm all files with File Service (in parallel)
        Mono<Void> confirmTask = Flux.fromIterable(attachments)
                .flatMap(att -> fileUploadClient.confirmFile(att.getFileId()))
                .then();

        // 2. Prepare Batch Request for Rust
        List<RustProcessRequest> rustRequests = attachments.stream()
                .filter(att -> isSupportedByRust(att.getFilename()))
                .map(att -> new RustProcessRequest(att.getFileUrl(), att.getFilename()))
                .toList();

        // 3. Process Batch via Rust
        Mono<Map<String, String>> processingTask = rustClient.processBatch(rustRequests)
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
        return Mono.when(confirmTask) // Ensure confirmation happens
                .then(processingTask)
                .map(resultsMap -> {
                    List<ProcessedFileContent> processedFiles = attachments.stream()
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
                    
                    List<FileUploadResult> synthesizedUploads = attachments.stream()
                            .map(att -> new FileUploadResult(att.getFileUrl(), att.getFilename()))
                            .toList();

                    return new FileProcessingResult(synthesizedUploads, processedFiles, fileRefs);
                });
    }

    private String extractResultText(JsonNode node) {
        // 🔥 FIX: Check lỗi cấp Item trước - Handle error case first
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
