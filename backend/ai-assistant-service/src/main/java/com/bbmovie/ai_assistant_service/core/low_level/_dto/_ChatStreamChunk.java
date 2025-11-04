package com.bbmovie.ai_assistant_service.core.low_level._dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class _ChatStreamChunk {

    private String type; // "assistant", "user", "tool", "system"
    private String content; // partial or complete text

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<_RagMovieDto> ragResults; // Optional — if RAG used

    public static _ChatStreamChunk assistant(String content) {
        return _ChatStreamChunk.builder()
                .type("assistant")
                .content(content)
                .build();
    }

    public static _ChatStreamChunk system(String content) {
        return _ChatStreamChunk.builder()
                .type("system")
                .content(content)
                .build();
    }

    public static _ChatStreamChunk ragResult(List<_RagMovieDto> movies) {
        return _ChatStreamChunk.builder()
                .type("rag_result")
                .ragResults(movies)
                .build();
    }
}
