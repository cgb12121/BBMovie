package bbmovie.transcode.temporal_orchestrator.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.transcode.nats-bridge", name = "enabled", havingValue = "true")
public class NatsClientConfiguration {

    @Bean(destroyMethod = "close")
    public Connection natsConnection(@Value("${nats.url:nats://localhost:4222}") String url) throws Exception {
        return Nats.connect(url);
    }
}
