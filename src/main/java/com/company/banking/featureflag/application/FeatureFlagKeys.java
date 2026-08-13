package com.company.banking.featureflag.application;

/**
 * Known flag keys. Add new keys here so application code does not scatter string literals.
 */
public final class FeatureFlagKeys {

    /** Placeholder for a dual-path account create rollout (default off). */
    public static final String NEW_ACCOUNT_FLOW = "new-account-flow";

    private FeatureFlagKeys() {}
}
