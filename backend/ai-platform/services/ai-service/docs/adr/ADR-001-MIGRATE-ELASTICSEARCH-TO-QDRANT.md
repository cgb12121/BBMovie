# ADR-001: Migrate từ Elasticsearch sang Qdrant cho RAG

## Status
**Proposed** - 2025-12-26

## Context

### Vấn đề hiện tại

AI Service hiện đang sử dụng **Elasticsearch** làm vector database cho RAG (Retrieval-Augmented Generation) với các đặc điểm:

1. **Resource Consumption cao**:
   - Elasticsearch chạy trên JVM, ngốn RAM rất nhiều (1-1.5 GB khi idle, chưa làm gì)
   - Overhead lớn cho use case đơn giản: chỉ cần vector search cho RAG
   - Elasticsearch là search engine đa năng → overkill cho vector-only RAG use case

2. **Architecture mismatch**:
   - AI Service chỉ cần **vector similarity search** cho RAG retrieval
   - Không cần full-text search, analytics, hay logging features của Elasticsearch
   - Elasticsearch được thiết kế cho nhiều use cases → complexity không cần thiết

3. **Performance**:
   - Elasticsearch kNN search không được optimize tốt như vector DB chuyên dụng
   - Latency cao hơn → ảnh hưởng đến RAG response time

### Use Case hiện tại

AI Service sử dụng Elasticsearch cho:
- **RAG Retrieval**: Tìm movie context tương tự với user query dựa trên embedding vectors
- **Conversation Indexing**: Lưu trữ conversation fragments với embeddings để tìm context liên quan
- **kNN Search**: k-nearest neighbors search trên movie embeddings và conversation embeddings
- **Hybrid Search**: Tìm kiếm trên nhiều collections (movies, conversations)

### RAG Flow

```
User Query
    ↓
Generate Embedding (Ollama/DJL)
    ↓
kNN Search trên Elasticsearch
    ↓
Retrieve Top-K movies/conversations
    ↓
Build Context Text
    ↓
Send to LLM (with context)
```

## Decision

Chúng ta sẽ **migrate từ Elasticsearch sang Qdrant** cho RAG operations:

1. **Qdrant là Vector DB chuyên dụng**:
   - Được thiết kế đặc biệt cho vector similarity search
   - Native support cho kNN search với performance cao
   - Lightweight: chỉ cần ~100 MB RAM khi idle (so với 1-1.5 GB của Elasticsearch)

2. **Resource Efficiency**:
   - Giảm RAM usage từ 1-1.5 GB xuống ~100 MB (tiết kiệm ~90% RAM)
   - Phù hợp với microservices architecture
   - Có thể chạy nhiều AI services trên cùng server

3. **RAG Performance**:
   - Vector search nhanh hơn → RAG retrieval nhanh hơn
   - Lower latency → better user experience
   - gRPC API hiệu quả hơn HTTP REST

4. **Simplified Architecture**:
   - Qdrant chỉ làm vector search → code đơn giản hơn
   - Không cần maintain Elasticsearch cluster complexity
   - Dễ scale và maintain

## Consequences

### ✅ Advantages

1. **Resource Efficiency**:
   - Giảm RAM usage từ 1-1.5 GB xuống ~100 MB (~90% reduction)
   - Phù hợp với resource-constrained environments
   - Có thể chạy nhiều services trên cùng server

2. **RAG Performance**:
   - Vector search nhanh hơn → faster RAG retrieval
   - Lower latency cho user queries
   - Better throughput cho concurrent requests

3. **Simplicity**:
   - Code đơn giản hơn (không cần Elasticsearch client complexity)
   - Configuration ít hơn
   - Dễ debug và maintain

4. **Cost**:
   - Giảm infrastructure cost (ít RAM hơn)
   - Docker image nhỏ hơn → faster deployment

5. **RAG Quality**:
   - Faster retrieval → có thể tăng `topK` để improve context quality
   - Lower latency → better user experience

### ⚠️ Trade-offs

1. **Mất Full-Text Search**:
   - Qdrant chỉ làm vector search, không có full-text search
   - **Mitigation**: RAG chỉ cần vector search, không cần full-text. Nếu cần full-text, có thể dùng PostgreSQL full-text search riêng.

2. **Ecosystem**:
   - Elasticsearch có ecosystem lớn hơn (Kibana, monitoring tools)
   - Qdrant ecosystem nhỏ hơn nhưng đủ cho RAG use case

3. **Migration Effort**:
   - Cần migrate data từ Elasticsearch sang Qdrant (movies, conversations)
   - Cần refactor `RagServiceImpl` và related code
   - **Mitigation**: Có thể chạy song song trong transition period

4. **Learning Curve**:
   - Team cần học Qdrant API
   - **Mitigation**: Qdrant API đơn giản hơn Elasticsearch, có Java client tốt

5. **Multi-Collection Support**:
   - Qdrant hỗ trợ multiple collections (movies, conversations) → không có vấn đề
   - Có thể query nhiều collections trong cùng request

## Implementation Details

### Architecture

```
┌─────────────────┐
│   AI Service    │
│  (RAG Service)  │
└────────┬────────┘
         │
         │ gRPC (port 6334)
         ▼
┌─────────────────┐
│   Qdrant DB     │
│  (Vector Store) │
│   ~100 MB RAM   │
│                 │
│ Collections:    │
│ - movies        │
│ - conversations │
└─────────────────┘
```

### Key Components

1. **QdrantConfig** (cần implement):
   - `QdrantClient` bean với gRPC connection
   - Configuration: `ai.rag.qdrant.host`, `ai.rag.qdrant.port`, `ai.rag.qdrant.api-key`

2. **QdrantRagService** (refactor từ `RagServiceImpl`):
   - Thay thế `ElasticsearchAsyncClient` bằng `QdrantClient`
   - Methods: `retrieveMovieContext()`, `indexConversationFragment()`
   - Native kNN search thay vì Elasticsearch kNN

3. **Collection Management**:
   - Collection `movies`: Movie embeddings cho RAG retrieval
   - Collection `conversations`: Conversation fragment embeddings
   - Vector size: phụ thuộc vào embedding model (e.g., 384 cho all-minilm)

4. **Data Migration**:
   - Export movie embeddings từ Elasticsearch
   - Export conversation fragments từ Elasticsearch
   - Transform format (Elasticsearch → Qdrant points)
   - Import vào Qdrant collections

### Code Changes

#### Configuration

```java
// New: QdrantConfig.java
@Configuration
@ConfigurationProperties(prefix = "ai.rag.qdrant")
public class QdrantConfig {
    
    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, !apiKey.isBlank());
        if (!apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }
}
```

#### Service Layer

```java
// Refactor: RagServiceImpl.java
@Service
public class RagServiceImpl implements RagService {
    
    private final QdrantClient qdrantClient; // Thay ElasticsearchAsyncClient
    private final EmbeddingModel embeddingModel;
    
    @Override
    public Mono<RagRetrievalResult> retrieveMovieContext(UUID sessionId, String query, int topK) {
        return embedText(sessionId, query)
                .flatMap(vector -> qdrantKnnSearch(sessionId, vector, topK, "movies"))
                .map(movies -> buildContext(movies));
    }
    
    private Mono<List<RagMovieDto>> qdrantKnnSearch(
            UUID sessionId, float[] embedding, int topK, String collection) {
        List<Float> vector = Arrays.stream(embedding)
                .boxed()
                .collect(Collectors.toList());
        
        SearchPoints searchPoints = SearchPoints.newBuilder()
                .collectionName(collection)
                .vector(vector)
                .top(topK)
                .withPayload(true)
                .build();
        
        return Mono.fromCallable(() -> {
            SearchResponse response = qdrantClient.searchBlocking(searchPoints);
            return response.getResult().stream()
                    .map(this::toRagMovieDto)
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    public Mono<Void> indexConversationFragment(...) {
        // Index conversation với Qdrant
        PointStruct point = PointStruct.newBuilder()
                .id(UuidCreator.getTimeOrderedEpoch().toUuid().toString())
                .vector(embedding)
                .payload(payload)
                .build();
        
        return Mono.fromCallable(() -> {
            qdrantClient.upsertBlocking(UpsertPoints.newBuilder()
                    .collectionName("conversations")
                    .points(List.of(point))
                    .build());
            return null;
        });
    }
}
```

#### Configuration Properties

```properties
# Qdrant Configuration
ai.rag.qdrant.host=localhost
ai.rag.qdrant.port=6334
ai.rag.qdrant.api-key=

# Keep Elasticsearch config for migration period (optional)
# ai.rag.elasticsearch.host=localhost
# ai.rag.elasticsearch.port=9200
```

### Migration Strategy

1. **Phase 1: Parallel Run** (Optional):
   - Implement Qdrant service
   - Run both Elasticsearch và Qdrant
   - Compare results để verify correctness

2. **Phase 2: Data Migration**:
   - Export all movie embeddings từ Elasticsearch
   - Export all conversation fragments từ Elasticsearch
   - Transform và import vào Qdrant collections
   - Verify data integrity

3. **Phase 3: Switch**:
   - Update configuration: switch to Qdrant
   - Deploy new version
   - Monitor RAG performance và errors

4. **Phase 4: Cleanup**:
   - Remove Elasticsearch dependencies
   - Remove Elasticsearch configuration
   - Remove old code

### RAG Flow (After Migration)

```
User Query
    ↓
Generate Embedding (Ollama/DJL)
    ↓
kNN Search trên Qdrant (gRPC)
    ↓
Retrieve Top-K movies/conversations
    ↓
Build Context Text
    ↓
Send to LLM (with context)
```

## Alternatives Considered

### Alternative 1: Keep Elasticsearch
- **Pros**: Đã có sẵn, team đã quen
- **Cons**: Ngốn RAM, overkill cho vector-only RAG use case
- **Rejected**: Resource cost quá cao

### Alternative 2: Weaviate
- **Pros**: Vector DB tốt, có GraphQL API, hỗ trợ RAG tốt
- **Cons**: Nặng hơn Qdrant (~200-300 MB RAM), phức tạp hơn
- **Rejected**: Qdrant nhẹ hơn và đủ cho use case

### Alternative 3: Milvus
- **Pros**: Vector DB mạnh, scale tốt, hỗ trợ RAG tốt
- **Cons**: Nặng hơn Qdrant, phức tạp hơn (cần etcd, MinIO)
- **Rejected**: Overkill cho single-node deployment

### Alternative 4: PostgreSQL + pgvector
- **Pros**: Không cần service riêng, tích hợp với existing DB
- **Cons**: Performance kém hơn vector DB chuyên dụng, phức tạp hơn
- **Rejected**: Performance và simplicity không bằng Qdrant cho RAG

### Alternative 5: Chroma
- **Pros**: Vector DB nhẹ, dễ dùng, hỗ trợ RAG tốt
- **Cons**: Ecosystem nhỏ hơn Qdrant, ít features hơn
- **Rejected**: Qdrant có ecosystem tốt hơn và performance tốt hơn

## References

- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [Qdrant Java Client](https://github.com/qdrant/qdrant-java-client)
- [Qdrant for RAG](https://qdrant.tech/articles/rag-pipeline/)
- [Qdrant vs Elasticsearch for Vector Search](https://qdrant.tech/benchmarks/)
- Current implementation: `backend/ai-service/src/main/java/com/bbmovie/ai_assistant_service/service/impl/RagServiceImpl.java`
- Elasticsearch config: `backend/ai-service/src/main/java/com/bbmovie/ai_assistant_service/config/elasticsearch/ESProperties.java`
- Docker compose: `backend/docker-compose.yml` (qdrant service)

