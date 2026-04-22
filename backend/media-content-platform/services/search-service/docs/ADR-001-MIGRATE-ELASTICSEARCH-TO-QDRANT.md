# ADR-001: Migrate từ Elasticsearch sang Qdrant cho Vector Search

## Status
**Proposed** - 2025-12-26

## Context

### Vấn đề hiện tại

Search Service hiện đang sử dụng **Elasticsearch** làm vector database cho semantic search (similarity search) với các đặc điểm:

1. **Resource Consumption cao**:
   - Elasticsearch chạy trên JVM, ngốn RAM rất nhiều (1-1.5 GB khi idle, chưa làm gì)
   - Overhead lớn cho use case đơn giản: chỉ cần vector search (kNN)
   - Elasticsearch là search engine đa năng (full-text, analytics, logging) → overkill cho vector-only use case

2. **Architecture mismatch**:
   - Search Service chỉ cần **vector similarity search** (kNN)
   - Không cần full-text search phức tạp, analytics, hay logging features
   - Elasticsearch được thiết kế cho nhiều use cases → complexity không cần thiết

3. **Performance**:
   - Elasticsearch kNN search không được optimize tốt như vector DB chuyên dụng
   - Latency cao hơn so với vector DB native

### Use Case hiện tại

Search Service sử dụng Elasticsearch cho:
- **Similarity Search**: Tìm movies tương tự dựa trên embedding vectors
- **kNN Search**: k-nearest neighbors search với filters (type, age, region)
- **Index Management**: Lưu trữ movie documents với embedding vectors

## Decision

Chúng ta sẽ **migrate từ Elasticsearch sang Qdrant** cho vector search operations:

1. **Qdrant là Vector DB chuyên dụng**:
   - Được thiết kế đặc biệt cho vector similarity search
   - Native support cho kNN search với performance cao
   - Lightweight: chỉ cần ~100 MB RAM khi idle (so với 1-1.5 GB của Elasticsearch)

2. **Resource Efficiency**:
   - Giảm RAM usage từ 1-1.5 GB xuống ~100 MB (tiết kiệm ~90% RAM)
   - Phù hợp với microservices architecture (mỗi service nhẹ, độc lập)

3. **Simplified Architecture**:
   - Qdrant chỉ làm vector search → code đơn giản hơn
   - Không cần maintain Elasticsearch cluster complexity
   - gRPC API nhanh và hiệu quả hơn HTTP REST

4. **Maintainability**:
   - Qdrant có API đơn giản, dễ maintain
   - Ít dependencies, ít configuration
   - Docker image nhỏ hơn Elasticsearch

## Consequences

### ✅ Advantages

1. **Resource Efficiency**:
   - Giảm RAM usage từ 1-1.5 GB xuống ~100 MB (~90% reduction)
   - Phù hợp với resource-constrained environments
   - Có thể chạy nhiều services trên cùng server

2. **Performance**:
   - Vector search nhanh hơn (native kNN optimization)
   - Lower latency cho similarity queries
   - gRPC protocol hiệu quả hơn HTTP REST

3. **Simplicity**:
   - Code đơn giản hơn (không cần Elasticsearch client complexity)
   - Configuration ít hơn
   - Dễ debug và maintain

4. **Cost**:
   - Giảm infrastructure cost (ít RAM hơn)
   - Docker image nhỏ hơn → faster deployment

### ⚠️ Trade-offs

1. **Mất Full-Text Search**:
   - Qdrant chỉ làm vector search, không có full-text search
   - **Mitigation**: Nếu cần full-text search, có thể dùng PostgreSQL full-text search hoặc giữ Elasticsearch riêng cho full-text

2. **Ecosystem**:
   - Elasticsearch có ecosystem lớn hơn (Kibana, monitoring tools)
   - Qdrant ecosystem nhỏ hơn nhưng đủ cho use case này

3. **Migration Effort**:
   - Cần migrate data từ Elasticsearch sang Qdrant
   - Cần refactor code (repository layer)
   - **Mitigation**: Có thể chạy song song trong transition period

4. **Learning Curve**:
   - Team cần học Qdrant API
   - **Mitigation**: Qdrant API đơn giản hơn Elasticsearch

## Implementation Details

### Architecture

```
┌─────────────────┐
│ Search Service  │
└────────┬────────┘
         │
         │ gRPC (port 6334)
         ▼
┌─────────────────┐
│   Qdrant DB     │
│  (Vector Store) │
│   ~100 MB RAM   │
└─────────────────┘
```

### Key Components

1. **QdrantConfig** (đã có sẵn):
   - `QdrantClient` bean với gRPC connection
   - Configuration: `qdrant.host`, `qdrant.port`, `qdrant.api-key`

2. **QdrantSearchRepository** (cần implement):
   - Thay thế `ElasticsearchSearchRepository`
   - Implement `SearchRepository` interface
   - Methods: `findAll()`, `findSimilar()` với kNN search

3. **Collection Management**:
   - Qdrant collections tương đương Elasticsearch indices
   - Collection name: `movies` (giống index name hiện tại)
   - Vector size: phụ thuộc vào embedding model (e.g., 384 cho all-minilm)

4. **Data Migration**:
   - Export data từ Elasticsearch
   - Transform format (Elasticsearch → Qdrant points)
   - Import vào Qdrant collection

### Code Changes

#### Repository Layer

```java
// New: QdrantSearchRepository.java
@Repository
@ConditionalOnProperty(name = "app.search.engine", havingValue = "qdrant")
public class QdrantSearchRepository implements SearchRepository {
    
    private final QdrantClient qdrantClient;
    private final String collectionName = "movies";
    
    @Override
    public <T> Mono<SearchResponse<T>> findSimilar(
            SearchCriteria criteria, 
            List<Float> queryVector, 
            Class<T> clazz) {
        // Qdrant kNN search
        return Mono.fromCallable(() -> {
            SearchPoints searchPoints = SearchPoints.newBuilder()
                .collectionName(collectionName)
                .vector(queryVector)
                .top(criteria.getSize())
                .withPayload(true)
                .build();
            
            SearchResponse response = qdrantClient.searchBlocking(searchPoints);
            return mapToElasticsearchFormat(response); // For compatibility
        });
    }
}
```

#### Service Layer

- `ESClientSearchService` → `QdrantSearchService` (hoặc rename thành generic `VectorSearchService`)
- Keep `EmbeddingService` unchanged (vẫn dùng Ollama/DJL)

#### Configuration

```properties
# Switch to Qdrant
app.search.engine=qdrant
qdrant.host=localhost
qdrant.port=6334
qdrant.api-key=

# Keep Elasticsearch config for migration period (optional)
# spring.elasticsearch.uris=http://localhost:9200
```

### Migration Strategy

1. **Phase 1: Parallel Run** (Optional):
   - Implement Qdrant repository
   - Run both Elasticsearch và Qdrant
   - Compare results để verify correctness

2. **Phase 2: Data Migration**:
   - Export all movie documents từ Elasticsearch
   - Transform và import vào Qdrant
   - Verify data integrity

3. **Phase 3: Switch**:
   - Update configuration: `app.search.engine=qdrant`
   - Deploy new version
   - Monitor performance và errors

4. **Phase 4: Cleanup**:
   - Remove Elasticsearch dependencies
   - Remove Elasticsearch configuration
   - Remove old repository code

## Alternatives Considered

### Alternative 1: Keep Elasticsearch
- **Pros**: Đã có sẵn, team đã quen
- **Cons**: Ngốn RAM, overkill cho vector-only use case
- **Rejected**: Resource cost quá cao

### Alternative 2: Weaviate
- **Pros**: Vector DB tốt, có GraphQL API
- **Cons**: Nặng hơn Qdrant (~200-300 MB RAM), phức tạp hơn
- **Rejected**: Qdrant nhẹ hơn và đủ cho use case

### Alternative 3: Milvus
- **Pros**: Vector DB mạnh, scale tốt
- **Cons**: Nặng hơn Qdrant, phức tạp hơn (cần etcd, MinIO)
- **Rejected**: Overkill cho single-node deployment

### Alternative 4: PostgreSQL + pgvector
- **Pros**: Không cần service riêng, tích hợp với existing DB
- **Cons**: Performance kém hơn vector DB chuyên dụng, phức tạp hơn
- **Rejected**: Performance và simplicity không bằng Qdrant

## References

- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [Qdrant Java Client](https://github.com/qdrant/qdrant-java-client)
- [Qdrant vs Elasticsearch for Vector Search](https://qdrant.tech/benchmarks/)
- Current implementation: `backend/search-service/src/main/java/com/bbmovie/search/repository/search/ElasticsearchSearchRepository.java`
- Qdrant config: `backend/search-service/src/main/java/com/bbmovie/search/config/vector/QdrantConfig.java`
- Docker compose: `backend/docker-compose.yml` (qdrant service)

