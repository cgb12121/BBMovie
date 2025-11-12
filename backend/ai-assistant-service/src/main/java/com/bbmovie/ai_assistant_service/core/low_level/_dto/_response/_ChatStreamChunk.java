package com.bbmovie.ai_assistant_service.core.low_level._dto._response;

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String type; // "assistant", "user", "tool", "system"

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String content; // partial or complete text

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String thinking;

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
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

    public static _ChatStreamChunk ragResults(List<_RagMovieDto> movies) {
        return _ChatStreamChunk.builder()
                .type("rag_result")
                .ragResults(movies)
                .build();
    }

    public static _ChatStreamChunk thinking(String thinking) {
        return _ChatStreamChunk.builder()
                .type("thinking")
                .thinking(thinking)
                .build();
    }
}
