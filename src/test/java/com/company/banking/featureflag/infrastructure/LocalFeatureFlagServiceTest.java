package com.company.banking.featureflag.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.banking.featureflag.application.FeatureFlagKeys;
import com.company.banking.featureflag.application.FeatureFlagService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalFeatureFlagServiceTest {

    @Test
    void returnsConfiguredFlagValue() {
        FeatureFlagService service = new LocalFeatureFlagService(
                Map.of(FeatureFlagKeys.NEW_ACCOUNT_FLOW, true));

        assertThat(service.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, "user-1", false)).isTrue();
    }

    @Test
    void unknownFlagUsesDefault() {
        FeatureFlagService service = new LocalFeatureFlagService(Map.of());

        assertThat(service.isEnabled("missing-flag", "user-1", true)).isTrue();
        assertThat(service.isEnabled("missing-flag", "user-1", false)).isFalse();
    }

    @Test
    void blankKeyUsesDefault() {
        FeatureFlagService service = new LocalFeatureFlagService(
                Map.of(FeatureFlagKeys.NEW_ACCOUNT_FLOW, true));

        assertThat(service.isEnabled("  ", "user-1", false)).isFalse();
        assertThat(service.isEnabled(null, "user-1", true)).isTrue();
    }

    @Test
    void copyOfFlagsIgnoresLaterMutationOfSourceMap() {
        Map<String, Boolean> source = new HashMap<>();
        source.put(FeatureFlagKeys.NEW_ACCOUNT_FLOW, false);
        FeatureFlagService service = new LocalFeatureFlagService(source);

        source.put(FeatureFlagKeys.NEW_ACCOUNT_FLOW, true);

        assertThat(service.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, "user-1", true)).isFalse();
    }
}
