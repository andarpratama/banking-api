package com.company.banking.featureflag.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.banking.featureflag.application.FeatureFlagKeys;
import com.company.banking.featureflag.application.FeatureFlagService;
import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves a LaunchDarkly flag can be toggled without constructing a new client (no restart).
 */
class LaunchDarklyFeatureFlagServiceTest {

    private TestData testData;
    private LDClient client;
    private FeatureFlagService service;

    @BeforeEach
    void setUp() {
        testData = TestData.dataSource();
        LDConfig config = new LDConfig.Builder()
                .dataSource(testData)
                .events(Components.noEvents())
                .logging(Components.noLogging())
                .build();
        client = new LDClient("sdk-test-offline", config);
        service = new LaunchDarklyFeatureFlagService(client);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void toggleChangesEvaluationWithoutNewClient() {
        testData.update(testData.flag(FeatureFlagKeys.NEW_ACCOUNT_FLOW).variationForAll(false));
        assertThat(service.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, "user-1", true)).isFalse();

        testData.update(testData.flag(FeatureFlagKeys.NEW_ACCOUNT_FLOW).variationForAll(true));
        assertThat(service.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, "user-1", false)).isTrue();
    }

    @Test
    void anonymousUserAndBlankKeyAreSafe() {
        testData.update(testData.flag(FeatureFlagKeys.NEW_ACCOUNT_FLOW).variationForAll(true));

        assertThat(service.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, null, false)).isTrue();
        assertThat(service.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, "  ", false)).isTrue();
        assertThat(service.isEnabled("  ", "user-1", false)).isFalse();
    }
}
