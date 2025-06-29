package com.example.bbmovieragrecommendation.service.rag;

import com.example.bbmovieragrecommendation.dto.ContextSnippet;
import com.example.bbmovieragrecommendation.dto.RAGQuery;
import com.example.bbmovieragrecommendation.dto.RAGResponse;
import com.example.bbmovieragrecommendation.service.llm.LLMClient;
import com.example.bbmovieragrecommendation.service.elastic.SearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RAGService {

    private final SearchClient searchClient;
    private final LLMClient llmClient;

    public Mono<RAGResponse> generateAnswer(RAGQuery q) {
        return searchClient.retrieve(q.getQuery(), q.getTopK())
            .collectList()
            .flatMap(snippets -> {
                String prompt = buildPrompt(q.getQuery(), snippets);
                return llmClient.complete(prompt)
                    .map(answer -> new RAGResponse(answer, snippets));
            });
    }

    private String buildPrompt(String question, List<ContextSnippet> snippets) {
        var sb = new StringBuilder("""
                Use the following context to answer:
                
                """);
        for (var s : snippets) {
            sb.append("- [").append(s.getType()).append("] ")
                    .append(s.getTitle()).append(": ")
                    .append(s.getText())
                    .append("\n");
        }
        sb.append("\nQuestion: ").append(question).append("\nAnswer:");
        return sb.toString();
    }
}
