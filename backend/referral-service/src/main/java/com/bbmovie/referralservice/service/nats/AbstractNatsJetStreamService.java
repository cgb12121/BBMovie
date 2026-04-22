package com.bbmovie.referralservice.service.nats;

import com.bbmovie.referralservice.config.NatsConfig;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import java.io.IOException;

@Log4j2
public abstract class AbstractNatsJetStreamService {

    @Autowired
    private ApplicationContext applicationContext;

    protected JetStream getJetStream() {
        NatsConfig.NatsConnectionFactory factory = applicationContext.getBean(NatsConfig.NatsConnectionFactory.class);
        Connection conn = factory.getConnection();
        if (conn == null) {
            return null;
        }
        try {
            return conn.jetStream();
        } catch (IOException e) {
            log.error("Failed to get JetStream context", e);
            return null;
        }
    }
}
