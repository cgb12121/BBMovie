package com.bbmovie.fileservice.dto.event;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import org.springframework.context.ApplicationEvent;

public class NatsConnectionEvent extends ApplicationEvent {

    private final transient Connection connection;
    private final ConnectionListener.Events type;

    public NatsConnectionEvent(Object source, Connection connection, ConnectionListener.Events type) {
        super(source);
        this.connection = connection;
        this.type = type;
    }

    public Connection connection() {
        return connection;
    }

    public ConnectionListener.Events type() {
        return type;
    }
}
