package com.example.bbmoviesearch.config.ai_vector;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder(new HttpHost("localhost", 9200, "http")).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        return new ElasticsearchClient(new RestClientTransport(restClient, jsonpMapper));
    }
}


@SuppressWarnings("all")
class SuppressWarningsPlaceholder {
    //TODO: Elasticsearch Java client clones or reinitializes its mapper internally when
// wrapped in custom SmartLifecycle setup ==> That clone lost the JavaTimeModule registration, so it couldn’t serialize LocalDateTime.
//@Log4j2
//@Configuration
//@ConditionalOnProperty(prefix = "vector.elastic", name = "enabled", havingValue = "true")
//public class ElasticsearchConfig {
//
//    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
//    private String moviesIndex;
//
//    private final ObjectMapper objectMapper;
//    private final EmbeddingModel embeddingModel;
//
//    @Autowired
//    public ElasticsearchConfig(ObjectMapper objectMapper, EmbeddingModel embeddingModel) {
//        this.objectMapper = objectMapper;
//        this.embeddingModel = embeddingModel;
//    }
//
//    @Bean
//    public SmartLifecycle elasticsearchLifecycle(ElasticsearchConnectionFactory factory) {
//        return factory;
//    }
//
//    @Bean
//    public ElasticsearchConnectionFactory elasticsearchConnection() {
//        return new ElasticsearchConnectionFactory(this.objectMapper, embeddingModel);
//    }
//
//
//    public class ElasticsearchConnectionFactory implements SmartLifecycle {
//
//        private final ExecutorService executor = Executors.newSingleThreadExecutor();
//        private final AtomicBoolean running = new AtomicBoolean(false);
//        private final AtomicReference<RestClient> restClientRef = new AtomicReference<>();
//        private final AtomicReference<ElasticsearchClient> clientRef = new AtomicReference<>();
//        private final AtomicReference<ElasticsearchVectorStore> vectorStoreRef = new AtomicReference<>();
//
//        @Value("${spring.elasticsearch.rest.uris:localhost:9200}")
//        private String esUri;
//
//        private final ObjectMapper objectMapper;  // NEW: Constructor-injected
//        private final EmbeddingModel embeddingModel;
//
//        public ElasticsearchConnectionFactory(ObjectMapper objectMapper, EmbeddingModel embeddingModel) {
//            this.objectMapper = objectMapper;
//            this.embeddingModel = embeddingModel;
//        }
//
//        public RestClient getRestClient() {
//            return restClientRef.get();
//        }
//
//        public ElasticsearchClient getClient() {
//            return clientRef.get();
//        }
//
//        public ElasticsearchVectorStore getVectorStore() {
//            return vectorStoreRef.get();
//        }
//
//        @Override
//        public void start() {
//            log.info("Starting Elasticsearch connection lifecycle...");
//            if (running.compareAndSet(false, true)) {
//                executor.submit(() -> {
//                    try {
//                        this.connectWithRetry();
//                    } catch (Exception e) {
//                        log.error("Elasticsearch connection thread crashed [{}]: {}", e.getClass().getName(), e.getMessage());
//                    }
//                });
//            }
//        }
//
//        private void connectWithRetry() {
//            RetryConfig config = RetryConfig.custom()
//                    .maxAttempts(Integer.MAX_VALUE)
//                    .intervalFunction(
//                            IntervalFunction.ofExponentialBackoff(Duration.ofMillis(2000), 2.0, Duration.ofSeconds(30))
//                    )
//                    .retryExceptions(Exception.class) // Retry on all ES connect errors
//                    .build();
//
//            Retry retry = Retry.of("es-connect", config);
//
//            Callable<RestClient> connect = Retry.decorateCallable(retry, () -> {
//                log.info("Trying to connect to Elasticsearch...");
//                HttpHost host = HttpHost.create("http://" + esUri);
//                return RestClient.builder(host).build();
//            });
//
//            while (running.get() && restClientRef.get() == null) {
//                try {
//                    RestClient restClient = connect.call();
//                    restClientRef.set(restClient);
//
//                    ObjectMapper mapper = objectMapper.copy()
//                            .registerModule(new JavaTimeModule())
//                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//                    JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);
//                    ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper);
//
//                    ElasticsearchClient client = new ElasticsearchClient(transport);
//                    clientRef.set(client);
//
//                    // Test connection
//                    client.info();
//
//                    // Setup VectorStore and schema
//                    setupVectorStore();
//
//                    log.info("Successfully connected to Elasticsearch");
//                    break;
//                } catch (Exception e) {
//                    log.error("Failed to connect to Elasticsearch, will retry: {}", e.getMessage());
//                }
//            }
//        }
//
//        private void setupVectorStore() {
//            try {
//                ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
//                options.setIndexName(moviesIndex);
//                options.setDimensions(384);
//                options.setEmbeddingFieldName("embedding");
//                options.setSimilarity(SimilarityFunction.cosine);
//
//                ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore
//                        .builder(getRestClient(), embeddingModel)
//                        .options(options)
//                        .initializeSchema(true)
//                        .build();
//                vectorStoreRef.set(vectorStore);
//                log.info("Created and initialized VectorStore for index: {}", moviesIndex);
//            } catch (Exception e) {
//                log.error("Error creating VectorStore: {}", e.getMessage());
//            }
//        }
//
//        @Override
//        public void stop() {
//            if (running.compareAndSet(true, false)) {
//                RestClient restClient = restClientRef.get();
//                if (restClient != null) {
//                    try {
//                        restClient.close();
//                    } catch (IOException e) {
//                        log.error("Error closing Elasticsearch client: {}", e.getMessage());
//                    }
//                    log.info("Elasticsearch connection closed");
//                }
//            }
//        }
//
//        @Override
//        public boolean isRunning() {
//            return running.get();
//        }
//
//        @Override
//        public int getPhase() {
//            return 100;  // Late phase: Start after core beans
//        }
//    }
//}
}