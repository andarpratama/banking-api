package com.company.banking.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature-flag provider settings. {@code local} uses {@link #flags}; {@code launchdarkly}
 * uses {@link #sdkKey} and ignores the YAML map at evaluation time.
 */
@ConfigurationProperties(prefix = "app.feature-flags")
public class FeatureFlagProperties {

    /**
     * {@code local} (default, tests/dev without a vendor) or {@code launchdarkly}.
     */
    private String provider = "local";

    /** LaunchDarkly server-side SDK key. Never commit a real value. */
    private String sdkKey = "";

    /**
     * Local flag map (YAML). Keys match LaunchDarkly flag keys when you promote a flag.
     */
    private Map<String, Boolean> flags = new HashMap<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSdkKey() {
        return sdkKey;
    }

    public void setSdkKey(String sdkKey) {
        this.sdkKey = sdkKey;
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public void setFlags(Map<String, Boolean> flags) {
        this.flags = flags != null ? flags : new HashMap<>();
    }
}
