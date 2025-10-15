package com.bbmovie.search.config.ai_vector;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class ElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient restClient(
            @Value("${elasticsearch.host:localhost}") String host,
            @Value("${elasticsearch.port:9200}") int port,
            @Value("${elasticsearch.scheme:http}") String scheme
    ) {
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

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
        return new ElasticsearchClient(new RestClientTransport(restClient, jsonpMapper));
    }
}
