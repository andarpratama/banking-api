package com.company.banking.featureflag.infrastructure;

import com.company.banking.featureflag.application.FeatureFlagService;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;

/**
 * LaunchDarkly adapter. Flag changes in the LD dashboard apply without restarting the API.
 */
public final class LaunchDarklyFeatureFlagService implements FeatureFlagService {

    private static final String ANONYMOUS_KEY = "anonymous";

    private final LDClientInterface ldClient;

    public LaunchDarklyFeatureFlagService(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    @Override
    public boolean isEnabled(String featureKey, String userId, boolean defaultValue) {
        if (featureKey == null || featureKey.isBlank()) {
            return defaultValue;
        }
        return ldClient.boolVariation(featureKey, contextFor(userId), defaultValue);
    }

    private static LDContext contextFor(String userId) {
        if (userId == null || userId.isBlank()) {
            return LDContext.builder(ANONYMOUS_KEY).anonymous(true).build();
        }
        return LDContext.builder(userId).build();
    }
}
