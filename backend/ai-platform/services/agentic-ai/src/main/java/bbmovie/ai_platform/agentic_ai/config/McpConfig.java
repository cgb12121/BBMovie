package bbmovie.ai_platform.agentic_ai.config;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.client.webflux.transport.WebFluxSseClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class McpConfig {

    @Value("${spring.ai.mcp.client.sse.connections.mcp-server.url:http://localhost:8081/mcp/sse}")
    private String mcpServerUrl;

    @Bean
    @Lazy
    public McpAsyncClient mcpAsyncClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        // Casting ObjectMapper to JsonMapper (Jackson 3)  => compatible with JacksonMcpJsonMapper
        JsonMapper jsonMapperImpl = (JsonMapper) objectMapper;
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(jsonMapperImpl);
        
        // Config Transport with WebClient.Builder, mapper and url
        WebFluxSseClientTransport transport = new WebFluxSseClientTransport(
                webClientBuilder,
                jsonMapper,
                mcpServerUrl
        );
        
        return McpClient.async(transport)
                .build();
    }

    @Bean
    public AsyncMcpToolCallbackProvider mcpToolCallbackProvider(McpAsyncClient mcpAsyncClient) {
        return AsyncMcpToolCallbackProvider.builder()
            .mcpClients(mcpAsyncClient)
            .build();
    }
}
