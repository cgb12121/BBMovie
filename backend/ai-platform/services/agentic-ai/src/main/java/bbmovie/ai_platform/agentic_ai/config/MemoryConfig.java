package bbmovie.ai_platform.agentic_ai.config;

import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.service.memory.HybridChatMemory;
import io.qdrant.client.QdrantClient;
import tools.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
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
            @Qualifier("rRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate, 
            MessageRepository messageRepository, ObjectMapper objectMapper,
            io.nats.client.Connection natsConnection) {
        // Hybrid memory: Redis (Short-term) + NATS Event (Long-term)
        return new HybridChatMemory(redisTemplate, messageRepository, objectMapper, natsConnection);
    }
    
    @Bean
    @Primary
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("agentic-collection")
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory)
                    .build()
                )
                .build();
    }
}
