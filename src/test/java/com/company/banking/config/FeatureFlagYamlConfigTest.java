package com.company.banking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards feature-flag defaults in {@code application.yml} (quick-win: LaunchDarkly / local toggles).
 */
class FeatureFlagYamlConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void applicationYmlDefaultsToLocalProviderWithNewAccountFlowOff() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml on classpath").isNotNull();

            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> app = (Map<String, Object>) root.get("app");
            Map<String, Object> featureFlags = (Map<String, Object>) app.get("feature-flags");

            assertThat(featureFlags.get("provider")).isEqualTo("${FEATURE_FLAGS_PROVIDER:local}");
            assertThat(featureFlags.get("sdk-key")).isEqualTo("${LAUNCHDARKLY_SDK_KEY:}");

            Map<String, Object> flags = (Map<String, Object>) featureFlags.get("flags");
            assertThat(flags.get("new-account-flow")).isEqualTo(false);
        }
    }
}
