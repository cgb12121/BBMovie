package com.bbmovie.search.dto.event;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;

public record NatsConnectionEvent(Connection connection, ConnectionListener.Events type) {}
