package com.company.banking.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.notification.domain.NotificationMessage;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class HttpNotificationPublisherTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void postSendsJsonAndAcceptsNoContent() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicInteger status = new AtomicInteger(204);
        startServer(status, body);

        HttpNotificationPublisher publisher = publisher();
        NotificationMessage message = sampleMessage();

        assertThatCode(() -> publisher.publish(message)).doesNotThrowAnyException();
        assertThat(body.get())
                .contains(message.recipientId().toString())
                .contains("TRANSFER_COMPLETED")
                .contains("Transfer completed");
    }

    @Test
    void clientErrorIsSwallowed() throws Exception {
        AtomicInteger status = new AtomicInteger(400);
        startServer(status, new AtomicReference<>());

        assertThatCode(() -> publisher().publish(sampleMessage())).doesNotThrowAnyException();
    }

    @Test
    void serverErrorPropagates() throws Exception {
        AtomicInteger status = new AtomicInteger(503);
        startServer(status, new AtomicReference<>());

        assertThatThrownBy(() -> publisher().publish(sampleMessage()))
                .isInstanceOf(RestClientResponseException.class);
    }

    private HttpNotificationPublisher publisher() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build();
        return new HttpNotificationPublisher(restClient, "/v1/notifications");
    }

    private void startServer(AtomicInteger status, AtomicReference<String> capturedBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/notifications", exchange -> {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            capturedBody.set(new String(bytes, StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
    }

    private static NotificationMessage sampleMessage() {
        return NotificationMessage.transferCompleted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "50.00",
                Instant.parse("2026-08-20T07:00:00Z")
        );
    }
}
