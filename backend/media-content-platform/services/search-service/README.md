# Search Service

The Search Service provides intelligent search capabilities for the BBMovie platform. It leverages Elasticsearch for full-text search and semantic search using AI/ML embeddings. The service processes video metadata events via NATS and provides both traditional keyword search and vector-based similarity search.

## Architecture Overview

The service is built with Spring WebFlux for reactive programming and integrates multiple technologies for search capabilities:
- Elasticsearch for traditional full-text search
- AI/ML embeddings for semantic search
- NATS for event-driven metadata updates
- Eureka for service discovery
- JWT-based authentication for secure access

## External Communications & Services

### 1. Elasticsearch Search Engine

The service uses Elasticsearch as the primary search backend with vector store capabilities.

**Connection Details**:
- **Endpoint**: `http://localhost:9200` (configurable via `spring.elasticsearch.uris`)
- **Authentication**: Username and password from configuration
- **Client Library**: co.elastic.clients:elasticsearch-java (version 8.15.5)

**Features**:
- Full-text search across movie metadata (title, description, genres)
- Vector search using AI-generated embeddings for semantic similarity
- Index named `movies` for movie documents
- k-NN (k-nearest neighbors) search for similarity matching
- Pagination support for large result sets

**Dependencies**:
- `org.springframework.boot:spring-boot-starter-data-elasticsearch`
- `org.springframework.ai:spring-ai-starter-vector-store-elasticsearch`

### 2. NATS Messaging System

The service consumes video metadata events from NATS to update search indexes.

**Connection Details**:
- **Host**: `nats://localhost:4222`
- **Client Library**: io.nats:jnats (version 2.19.0)
- **Connection Name**: `search-service`

**Resilience Features**:
- Infinite reconnection attempts (`maxReconnects(-1)`)
- Exponential backoff retry mechanism (2s → 4s → 8s → ... capped at 30s)
- Connection timeout: 5 seconds
- Reconnect wait: 10 seconds
- Ping interval: 30 seconds

**Subscriptions**:
- **Subject**: `video.metadata`
- Consumes video metadata updates to maintain search index
- Updates or creates movie documents based on received metadata
- Handles movie titles, descriptions, genres, posters, and other metadata

### 3. AI/ML Embedding Services

The service uses multiple AI/ML providers for generating text embeddings for semantic search.

**DJL (Deep Java Library) Integration**:
- **Provider**: DJL with PyTorch engine
- **Model**: PyTorch model zoo for embedding generation
- **GPU Support**: CUDA 11.7 native libraries for GPU acceleration
- **Hugging Face**: Tokenizers integration for text preprocessing

**Ollama Integration**:
- **Endpoint**: `http://localhost:11434` (configurable via `spring.ai.ollama.base-url`)
- **Model**: `all-minilm` model for embedding generation
- **Features**: Keep-alive, context window, batch processing

**Hugging Face Integration**:
- **API Key**: Configured via `HUGGING_FACE_BBMOVIE_API_KEY`
- **Tokenizers**: Hugging Face tokenizers for text processing

**Embedding Router**:
- Selects primary embedding service (DJL preferred)
- Falls back to other available services
- Reactive processing with Mono for asynchronous operations

**Dependencies**:
- `org.springframework.ai:spring-ai-starter-model-ollama`
- `ai.djl:api:0.31.1`
- `ai.djl.pytorch:pytorch-engine:0.31.0`
- `ai.djl.huggingface:tokenizers:0.31.0`

### 4. Eureka Service Discovery

The service registers itself with Eureka for service discovery in the microservices architecture.

**Connection Details**:
- **Eureka Server**: `http://localhost:8761/eureka/`
- **Service Name**: `bbmovie-search`

**Configuration**:
- Service registration enabled
- Registry fetching enabled
- Heartbeat and renewal intervals configured

**Dependencies**:
- `org.springframework.cloud:spring-cloud-starter-netflix-eureka-client`

### 5. JWT OAuth2 Authentication

The service validates JWT tokens for secure API access.

**Configuration**:
- Validates JWT tokens from the authentication service
- Secured endpoints require valid authentication tokens
- Reactive security with WebFlux

## API Endpoints

### Search Endpoints
- **GET** `/search`
  - Performs search based on query parameters
  - Supports semantic search using AI embeddings
  - Parameters: query, page, size, genre, year, etc.
  - Returns paginated search results

- **GET** `/search/all`
  - Retrieves all movies with pagination
  - Parameters: page (default: 0), size (default: 10)
  - Returns paginated list of all movies

## Data Model

### Movie Document
The search index contains the following fields:
- `id` - Unique movie identifier
- `title` - Movie title
- `description` - Movie description
- `genres` - List of movie genres
- `country` - Country of origin
- `type` - Movie type (e.g., film, series)
- `poster` - URL to movie poster
- `releaseDate` - Release date
- `embedding` - AI-generated embedding vector for semantic search

## Event Processing

### Video Metadata Updates
1. **Event Reception**: Receives `video.metadata` events from NATS
2. **Document Update**: Updates existing movie document or creates new one
3. **Embedding Generation**: Generates embeddings for new documents
4. **Index Storage**: Stores/replaces document in Elasticsearch index

## Configuration

### Environment Variables
- `ELASTICSEARCH_USERNAME` - Elasticsearch username
- `ELASTICSEARCH_PASSWORD` - Elasticsearch password
- `HUGGING_FACE_BBMOVIE_API_KEY` - Hugging Face API key
- `OLLAMA_HOST` - Ollama server host (default: localhost:11434)

### Application Properties
```
server.port: (default from Spring Boot)
spring.application.name: bbmovie-search
spring.main.web-application-type: reactive
spring.ai.vectorstore.elasticsearch.index-name: movies
spring.elasticsearch.uris: http://localhost:9200
eureka.client.service-url.defaultZone: http://localhost:8761/eureka/
```

## Running the Service

Execute one of the following commands:

Windows:
```
.\run.bat
```

Linux/macOS:
```
./run.sh
```

## Security Features

- JWT-based authentication for API endpoints
- Secure parameter validation
- Reactive security with Spring WebFlux
- Protected against injection attacks