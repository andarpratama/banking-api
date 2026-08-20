package com.company.banking.config;

import com.company.banking.notification.application.NotificationPublisher;
import com.company.banking.notification.infrastructure.HttpNotificationPublisher;
import com.company.banking.notification.infrastructure.LoggingNotificationPublisher;
import com.company.banking.notification.infrastructure.ResilientNotificationPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {

    public static final String CIRCUIT_BREAKER_NAME = "notification";

    @Bean
    @ConditionalOnProperty(prefix = "app.notification", name = "provider", havingValue = "log", matchIfMissing = true)
    public NotificationPublisher loggingNotificationPublisher() {
        return new LoggingNotificationPublisher();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.notification", name = "provider", havingValue = "http")
    public NotificationPublisher resilientHttpNotificationPublisher(
            NotificationProperties properties,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        requireHttpBaseUrl(properties);
        HttpNotificationPublisher http = new HttpNotificationPublisher(
                httpNotificationRestClient(properties),
                properties.getHttp().getPath()
        );
        return new ResilientNotificationPublisher(
                circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME),
                http,
                new LoggingNotificationPublisher()
        );
    }

    RestClient httpNotificationRestClient(NotificationProperties properties) {
        NotificationProperties.Http http = properties.getHttp();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(http.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(http.getReadTimeout());
        return RestClient.builder()
                .baseUrl(properties.getHttp().getBaseUrl().trim())
                .requestFactory(factory)
                .build();
    }

    void requireHttpBaseUrl(NotificationProperties properties) {
        if (!StringUtils.hasText(properties.getHttp().getBaseUrl())) {
            throw new IllegalStateException(
                    "NOTIFICATION_HTTP_BASE_URL is required when app.notification.provider=http");
        }
    }
}
