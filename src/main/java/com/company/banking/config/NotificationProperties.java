package com.company.banking.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbound notification delivery. {@code log} is the T-052 stub; {@code http} POSTs JSON
 * through a Resilience4j circuit breaker (T-094).
 */
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    /**
     * {@code log} (default) or {@code http}.
     */
    private String provider = "log";

    private final Http http = new Http();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Http getHttp() {
        return http;
    }

    public static class Http {

        /** Vendor base URL, e.g. {@code https://notify.example.com}. Required when provider=http. */
        private String baseUrl = "";

        private String path = "/v1/notifications";

        private Duration connectTimeout = Duration.ofSeconds(1);

        private Duration readTimeout = Duration.ofSeconds(2);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl != null ? baseUrl : "";
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path != null && !path.isBlank() ? path : "/v1/notifications";
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(1);
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout != null ? readTimeout : Duration.ofSeconds(2);
        }
    }
}
