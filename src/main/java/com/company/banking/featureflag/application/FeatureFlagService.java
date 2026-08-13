package com.company.banking.featureflag.application;

/**
 * Application port for boolean feature flags (kill switches, gradual rollout).
 *
 * <p>Call from application services — not controllers — so HTTP stays free of
 * rollout rules. Example:
 *
 * <pre>{@code
 * if (featureFlagService.isEnabled(FeatureFlagKeys.NEW_ACCOUNT_FLOW, userId, false)) {
 *     return experimentalAccountService.create(request);
 * }
 * return accountService.create(request);
 * }</pre>
 *
 * <p>Local provider reads {@code app.feature-flags.flags} (restart to change).
 * LaunchDarkly provider evaluates remotely without a redeploy.
 */
public interface FeatureFlagService {

    /**
     * @param featureKey flag key in LaunchDarkly / local YAML
     * @param userId targeting key; blank is treated as anonymous
     * @param defaultValue used when the flag is unknown or the vendor is unavailable
     */
    boolean isEnabled(String featureKey, String userId, boolean defaultValue);
}
