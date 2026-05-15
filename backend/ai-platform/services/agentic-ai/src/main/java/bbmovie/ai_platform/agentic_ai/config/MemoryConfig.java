package bbmovie.ai_platform.agentic_ai.config;

import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.service.memory.HybridChatMemory;
import io.nats.client.JetStream;
import io.qdrant.client.QdrantClient;
import tools.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class MemoryConfig {

    @Bean
    @Primary
    public ChatMemory chatMemory(
            @Qualifier("rRedisTemplate") // Redis Template
            ReactiveRedisTemplate<String, String> redisTemplate, 
            MessageRepository messageRepository, ObjectMapper objectMapper,
            JetStream jetStream) {
        // Hybrid memory: Redis (Short-term) + NATS Event (Long-term)
        return new HybridChatMemory(redisTemplate, messageRepository, objectMapper, jetStream);
    }
    
    @Bean
    @Primary
    public VectorStore vectorStore(QdrantClient qdrantClient, @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("agentic-collection")
                .build();
    }
}
