package com.bbmovie.auth.unit.service.nats;

import com.bbmovie.auth.dto.event.NatsConnectionEvent;
import com.bbmovie.auth.service.nats.EmailEventProducer;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailEventProducerTest {

    @Mock
    private Connection connection;

    @Mock
    private JetStream jetStream;

    private EmailEventProducer emailEventProducer;

    @BeforeEach
    void setUp() {
        emailEventProducer = new EmailEventProducer();
    }

    @Test
    void onNatsConnection_WhenConnected_ShouldInitializeJetStream() throws IOException {
        // Given
        NatsConnectionEvent event = new NatsConnectionEvent(connection, ConnectionListener.Events.CONNECTED);

        when(connection.jetStream()).thenReturn(jetStream);

        // When
        emailEventProducer.onNatsConnection(event);

        // Then
        verify(connection).jetStream();
    }

    @Test
    void onNatsConnection_WhenReconnected_ShouldInitializeJetStream() throws IOException {
        // Given
        NatsConnectionEvent event = new NatsConnectionEvent(connection, ConnectionListener.Events.RECONNECTED);

        when(connection.jetStream()).thenReturn(jetStream);

        // When
        emailEventProducer.onNatsConnection(event);

        // Then
        verify(connection).jetStream();
    }

    @Test
    void sendMagicLinkOnRegistration_WhenJetStreamAvailable_ShouldPublishEvent() throws Exception {
        // Given
        NatsConnectionEvent event = new NatsConnectionEvent(connection, ConnectionListener.Events.CONNECTED);
        when(connection.jetStream()).thenReturn(jetStream);

        // Initialize jetStream
        emailEventProducer.onNatsConnection(event);

        // When
        emailEventProducer.sendMagicLinkOnRegistration("test@example.com", "verification-token");

        // Then
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(jetStream).publish(subjectCaptor.capture(), dataCaptor.capture());
        assertEquals("auth.registration", subjectCaptor.getValue());

        String eventData = new String(dataCaptor.getValue());
        assertTrue(eventData.contains("test@example.com"));
        assertTrue(eventData.contains("verification-token"));
    }

    @Test
    void sendMagicLinkOnForgotPassword_WhenJetStreamAvailable_ShouldPublishEvent() throws Exception {
        // Given
        NatsConnectionEvent event = new NatsConnectionEvent(connection, ConnectionListener.Events.CONNECTED);
        when(connection.jetStream()).thenReturn(jetStream);

        // Initialize jetStream
        emailEventProducer.onNatsConnection(event);

        // When
        emailEventProducer.sendMagicLinkOnForgotPassword("test@example.com", "reset-token");

        // Then
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(jetStream).publish(subjectCaptor.capture(), dataCaptor.capture());
        assertEquals("auth.forgot_password", subjectCaptor.getValue());

        String eventData = new String(dataCaptor.getValue());
        assertTrue(eventData.contains("test@example.com"));
        assertTrue(eventData.contains("reset-token"));
    }

    @Test
    void sendOtp_WhenJetStreamAvailable_ShouldPublishEvent() throws Exception {
        // Given
        NatsConnectionEvent event = new NatsConnectionEvent(connection, ConnectionListener.Events.CONNECTED);
        when(connection.jetStream()).thenReturn(jetStream);

        // Initialize jetStream
        emailEventProducer.onNatsConnection(event);

        // When
        emailEventProducer.sendOtp("1234567890", "123456");

        // Then
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(jetStream).publish(subjectCaptor.capture(), dataCaptor.capture());
        assertEquals("auth.otp", subjectCaptor.getValue());

        String eventData = new String(dataCaptor.getValue());
        assertTrue(eventData.contains("1234567890"));
        assertTrue(eventData.contains("123456"));
    }

    @Test
    void sendMagicLinkOnRegistration_WhenJetStreamIsNull_ShouldNotPublishEvent() {
        // Given - JetStream is not initialized

        // When
        emailEventProducer.sendMagicLinkOnRegistration("test@example.com", "verification-token");

        // Then
        // Verify that publish was never called
        verifyNoInteractions(connection);
    }
}