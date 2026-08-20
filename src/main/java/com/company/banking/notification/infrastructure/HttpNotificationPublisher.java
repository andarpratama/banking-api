package com.company.banking.notification.infrastructure;

import com.company.banking.notification.application.NotificationPublisher;
import com.company.banking.notification.domain.NotificationMessage;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * POSTs notification JSON to a vendor HTTP endpoint. 4xx is logged and swallowed (payload bug,
 * vendor is up). 5xx and I/O errors propagate so the circuit breaker can open.
 */
public final class HttpNotificationPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpNotificationPublisher.class);

    private final RestClient restClient;
    private final String path;

    public HttpNotificationPublisher(RestClient restClient, String path) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.path = requireText(path, "path");
    }

    @Override
    public void publish(NotificationMessage message) {
        Objects.requireNonNull(message, "message");
        try {
            restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(NotificationHttpPayload.from(message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                log.warn(
                        "Notification vendor rejected payload (ignored): status={} type={} recipientId={}",
                        ex.getStatusCode().value(),
                        message.type(),
                        message.recipientId()
                );
                return;
            }
            throw ex;
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    record NotificationHttpPayload(
            String recipientId,
            String type,
            String title,
            String body,
            String occurredAt
    ) {
        static NotificationHttpPayload from(NotificationMessage message) {
            return new NotificationHttpPayload(
                    message.recipientId().toString(),
                    message.type().name(),
                    message.title(),
                    message.body(),
                    message.occurredAt().toString()
            );
        }
    }
}
