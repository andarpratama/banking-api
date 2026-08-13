package com.company.banking.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ObservabilityConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ObservabilityConfig.class);

    @Test
    void doesNotCreateSdkWhenOtelDisabled() {
        runner.withPropertyValues("otel.sdk.disabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(OpenTelemetrySdk.class));
    }
}
