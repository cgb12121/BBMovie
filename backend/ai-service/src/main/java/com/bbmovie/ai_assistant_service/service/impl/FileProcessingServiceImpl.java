package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.service.FileProcessingService;
import com.bbmovie.ai_assistant_service.service.FileUploadClient;
import com.bbmovie.ai_assistant_service.service.WhisperService;
import com.bbmovie.ai_assistant_service.utils.FileTypeUtils;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of a FileProcessingService that handles different file types.
 * <p>
 * Processing strategy:
 * - Images: Upload only (AI models can process image URLs directly)
 * - PDFs: Upload + extract text (future implementation)
 * - Text files: Upload + extract text (future implementation)
 * - Audio: Upload and transcribe using WhisperService
 * <p>
 * Why this implementation:
 * - Separates audio processing (blocking) from other file types
 * - Uses reactive streams for parallel processing where possible
 * - Handles errors gracefully per file
 */
@Service
@RequiredArgsConstructor
public class FileProcessingServiceImpl implements FileProcessingService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(FileProcessingServiceImpl.class);

    private final FileUploadClient fileUploadClient;
    private final WhisperService whisperService;

    @Override
    public Mono<FileProcessingResult> processFiles(List<FilePart> files, Jwt jwt) {
        if (files == null || files.isEmpty()) {
            return Mono.just(new FileProcessingResult(List.of(), List.of(), List.of()));
        }

        log.info("Processing {} files", files.size());

        // Separate files by type
        List<FilePart> imageFiles = new ArrayList<>();
        List<FilePart> documentFiles = new ArrayList<>();
        List<FilePart> audioFiles = new ArrayList<>();
        List<FilePart> textFiles = new ArrayList<>();

        for (FilePart file : files) {
            String contentType = file.headers().getContentType() != null
                    ? Objects.requireNonNull(file.headers().getContentType()).toString()
                    : "";
            String filename = file.filename().toLowerCase();

            if (contentType.startsWith("image/") || FileTypeUtils.isImageFile(filename)) {
                imageFiles.add(file);
            } else if (contentType.contains("pdf") || FileTypeUtils.isDocumentFile(filename)) {
                documentFiles.add(file);
            } else if (contentType.startsWith("audio/") || FileTypeUtils.isAudioFile(filename)) {
                audioFiles.add(file);
            } else if (contentType.contains("text/") || filename.endsWith(".txt")) {
                textFiles.add(file);
            } else {
                // Default to document for unknown types
                documentFiles.add(file);
            }
        }

        // Process files in parallel where possible
        Mono<List<FileUploadResult>> imagesMono = uploadFiles(imageFiles, jwt, fileUploadClient::uploadImage);
        Mono<List<FileUploadResult>> documentsMono = uploadFiles(documentFiles, jwt, fileUploadClient::uploadDocument);
        Mono<List<FileUploadResult>> textMono = uploadFiles(textFiles, jwt, fileUploadClient::uploadDocument);

        // Process audio files for both upload and transcription
        Mono<List<ProcessedFileContent>> audioResultsMono = processAudioFiles(audioFiles, jwt);

        // Combine all results
        return Mono.zip(imagesMono, documentsMono, textMono, audioResultsMono)
                .map(tuple -> {
                    List<FileUploadResult> allFiles = new ArrayList<>();
                    allFiles.addAll(tuple.getT1()); // images
                    allFiles.addAll(tuple.getT2()); // documents
                    allFiles.addAll(tuple.getT3()); // text

                    // Audio files are special because they return ProcessedFileContent, which
                    // contains the upload result info too
                    List<ProcessedFileContent> audioProcessed = tuple.getT4();

                    // Add audio upload results to the main list
                    // We need to reconstruct FileUploadResult from ProcessedFileContent or change
                    // how we collect them
                    // For now, let's just extract the upload info from ProcessedFileContent if
                    // needed,
                    // but wait, processAudioFiles returns ProcessedFileContent now.
                    // Let's adjust processAudioFiles to return both or just ProcessedFileContent
                    // and we map it.

                    // Actually, let's keep it simple. processAudioFiles will return
                    // List<ProcessedFileContent>
                    // We can extract FileUploadResult from it if we want to keep allFiles complete,
                    // but ProcessedFileContent has the URL.
                    // Let's create FileUploadResult from ProcessedFileContent for consistency in
                    // allFiles list
                    List<FileUploadResult> audioUploads = audioProcessed.stream()
                            .map(p -> new FileUploadResult(p.url(), p.filename()))
                            .toList();

                    allFiles.addAll(audioUploads);

                    List<String> fileReferences = allFiles.stream()
                            .map(result -> result.getPublicId() != null ? result.getPublicId() : result.getUrl())
                            .filter(ref -> ref != null && !ref.isEmpty())
                            .collect(Collectors.toList());

                    return new FileProcessingResult(allFiles, audioProcessed, fileReferences);
                });
    }

    /**
     * Uploads a list of files using the specified upload method.
     */
    private Mono<List<FileUploadResult>> uploadFiles(List<FilePart> files, Jwt jwt, FileUploadMethod uploadMethod) {

        if (files.isEmpty()) {
            return Mono.just(List.of());
        }

        return Flux.fromIterable(files)
                .flatMap(file -> uploadMethod.upload(file, jwt)
                        .doOnError(
                                error -> log.error("Failed to upload file {}: {}", file.filename(), error.getMessage()))
                        .onErrorResume(error -> Mono.empty())) // Continue processing other files on error
                .collectList();
    }

    /**
     * Processes audio files: uploads them and transcribes them.
     * Audio processing is done separately as it's CPU-intensive and blocking.
     */
    private Mono<List<ProcessedFileContent>> processAudioFiles(List<FilePart> audioFiles, Jwt jwt) {
        if (audioFiles.isEmpty()) {
            return Mono.just(List.of());
        }

        return Flux.fromIterable(audioFiles)
                .flatMap(audioFile -> {
                    // Upload the file first
                    Mono<FileUploadResult> uploadMono = fileUploadClient.uploadAudio(audioFile, jwt);

                    // Then transcribe it (this is blocking, so it's done separately)
                    Mono<String> transcriptionMono = whisperService.transcribe(audioFile)
                            .doOnSuccess(transcription -> log.info("Transcribed audio file {}: {} chars",
                                    audioFile.filename(), transcription.length()))
                            .doOnError(error -> log.error("Failed to transcribe audio file {}: {}",
                                    audioFile.filename(), error.getMessage()))
                            .onErrorReturn(""); // Continue even if transcription fails

                    // Combine upload and transcription
                    return Mono.zip(uploadMono, transcriptionMono)
                            .map(tuple -> {
                                FileUploadResult result = tuple.getT1();
                                String transcription = tuple.getT2();
                                String fileRef = result.getPublicId() != null ? result.getPublicId() : result.getUrl();

                                log.info("Audio file {} transcribed: {} chars", fileRef, transcription.length());

                                return new ProcessedFileContent(
                                        audioFile.filename(),
                                        "audio",
                                        fileRef,
                                        transcription);
                            });
                })
                .collectList();
    }

    /**
     * Functional interface for different file upload methods.
     */
    @FunctionalInterface
    private interface FileUploadMethod {
        Mono<FileUploadResult> upload(FilePart file, Jwt jwt);
    }
}
