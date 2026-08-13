package com.company.banking.config;

import com.company.banking.featureflag.application.FeatureFlagService;
import com.company.banking.featureflag.infrastructure.LaunchDarklyFeatureFlagService;
import com.company.banking.featureflag.infrastructure.LocalFeatureFlagService;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(FeatureFlagProperties.class)
public class FeatureFlagConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.feature-flags", name = "provider", havingValue = "local", matchIfMissing = true)
    public FeatureFlagService localFeatureFlagService(FeatureFlagProperties properties) {
        return new LocalFeatureFlagService(properties.getFlags());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.feature-flags", name = "provider", havingValue = "launchdarkly")
    public LDClient launchDarklyClient(FeatureFlagProperties properties) {
        if (!StringUtils.hasText(properties.getSdkKey())) {
            throw new IllegalStateException(
                    "LAUNCHDARKLY_SDK_KEY is required when app.feature-flags.provider=launchdarkly");
        }
        return new LDClient(properties.getSdkKey().trim(), new LDConfig.Builder().build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.feature-flags", name = "provider", havingValue = "launchdarkly")
    public FeatureFlagService launchDarklyFeatureFlagService(LDClient launchDarklyClient) {
        return new LaunchDarklyFeatureFlagService(launchDarklyClient);
    }
}
