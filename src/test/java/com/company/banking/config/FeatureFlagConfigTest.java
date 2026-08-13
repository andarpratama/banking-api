package com.company.banking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.featureflag.application.FeatureFlagKeys;
import com.company.banking.featureflag.application.FeatureFlagService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeatureFlagConfigTest {

    @Test
    void localProviderReadsYamlFlags() {
        FeatureFlagProperties properties = new FeatureFlagProperties();
        properties.setFlags(Map.of(FeatureFlagKeys.NEW_ACCOUNT_FLOW, true));

        FeatureFlagService service = new FeatureFlagConfig().localFeatureFlagService(properties);

        assertThat(service.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, "admin", false)).isTrue();
    }

    @Test
    void launchDarklyClientRequiresSdkKey() {
        FeatureFlagProperties properties = new FeatureFlagProperties();
        properties.setSdkKey("  ");

        assertThatThrownBy(() -> new FeatureFlagConfig().launchDarklyClient(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LAUNCHDARKLY_SDK_KEY");
    }
}
