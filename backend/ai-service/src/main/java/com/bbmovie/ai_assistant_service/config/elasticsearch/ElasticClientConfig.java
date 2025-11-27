package com.bbmovie.ai_assistant_service.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticClientConfig {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ElasticClientConfig.class);

    static {
        log.debug("Elasticsearch Java client version: {}", ElasticsearchAsyncClient.class.getPackage().getImplementationVersion());
    }

    @Bean("elasticsearchAsyncClient")
    public ElasticsearchAsyncClient asyncClient(RestClient restClient) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        return new ElasticsearchAsyncClient(new RestClientTransport(restClient, jsonpMapper));
    }

    @Bean(destroyMethod = "close")
    public RestClient restClient(ESProperties properties) {
        String host = properties.getHost();
        int port = properties.getPort();
        String scheme = properties.getScheme();
        return RestClient.builder(new HttpHost(host, port, scheme))
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(5000)
                                .setSocketTimeout(60000)
                                .setConnectionRequestTimeout(5000)
                )
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setMaxConnTotal(100)
                                .setMaxConnPerRoute(100)
                )
                .setFailureListener(new RestClient.FailureListener() {
                    @Override
                    public void onFailure(Node node) {
                        log.warn("Elasticsearch node failed: {}. Will retry on next request.", node.getHost());
                    }
                })
                .build();
    }
}
