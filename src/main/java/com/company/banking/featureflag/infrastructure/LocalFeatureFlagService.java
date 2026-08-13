package com.company.banking.featureflag.infrastructure;

import com.company.banking.featureflag.application.FeatureFlagService;
import java.util.HashMap;
import java.util.Map;

/**
 * YAML / properties-backed flags for local and test profiles (no LaunchDarkly account).
 * Changes require a process restart — use the LaunchDarkly adapter for live toggles.
 */
public final class LocalFeatureFlagService implements FeatureFlagService {

    private final Map<String, Boolean> flags;

    public LocalFeatureFlagService(Map<String, Boolean> flags) {
        this.flags = flags == null || flags.isEmpty()
                ? Map.of()
                : Map.copyOf(new HashMap<>(flags));
    }

    @Override
    public boolean isEnabled(String featureKey, String userId, boolean defaultValue) {
        if (featureKey == null || featureKey.isBlank()) {
            return defaultValue;
        }
        Boolean value = flags.get(featureKey);
        return value != null ? value : defaultValue;
    }
}
