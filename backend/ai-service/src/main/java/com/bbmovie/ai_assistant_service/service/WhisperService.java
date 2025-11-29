package com.bbmovie.ai_assistant_service.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface WhisperService {
    /**
     * Transcribes the given audio file reactively.
     *
     * @param audioFile A {@link FilePart} representing the uploaded audio file.
     * @return A {@link Mono} emitting the transcription text upon completion.
     */
    Mono<String> transcribe(FilePart audioFile);
}
