package com.bbmovie.ai_assistant_service.dto.response;

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
public class ChatStreamChunk {

    private String type; // "assistant", "user", "tool", "system"
    private String content; // partial or complete text

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String thinking; // AI's thinking process

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> metadata = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<RagMovieDto> ragResults; // Optional — if RAG used

    public static ChatStreamChunk assistant(String content) {
        return ChatStreamChunk.builder()
                .type("assistant")
                .content(content)
                .build();
    }

    public static ChatStreamChunk system(String content) {
        return ChatStreamChunk.builder()
                .type("system")
                .content(content)
                .build();
    }

    public static ChatStreamChunk ragResult(List<RagMovieDto> movies) {
        return ChatStreamChunk.builder()
                .type("rag_result")
                .ragResults(movies)
                .build();
    }
}
