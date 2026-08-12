package com.company.banking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityHeadersTest.PingController.class)
@Import(SecurityHeadersTest.HeaderSecurityConfig.class)
@ContextConfiguration(classes = SecurityHeadersTest.PingController.class)
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void responsesIncludeV1SecurityHeaders() throws Exception {
        mockMvc.perform(get("/ping").secure(true).accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", SecurityHeaders.X_CONTENT_TYPE_OPTIONS))
                .andExpect(header().string("X-Frame-Options", SecurityHeaders.X_FRAME_OPTIONS))
                .andExpect(header().string("Content-Security-Policy", SecurityHeaders.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("Referrer-Policy", SecurityHeaders.REFERRER_POLICY))
                .andExpect(header().string("Permissions-Policy", SecurityHeaders.PERMISSIONS_POLICY))
                .andExpect(header().string("Strict-Transport-Security", SecurityHeaders.HSTS_VALUE));
    }

    @Test
    void cspDeniesFramingAndObjects() {
        assertThat(SecurityHeaders.CONTENT_SECURITY_POLICY)
                .contains("frame-ancestors 'none'")
                .contains("object-src 'none'")
                .contains("default-src 'self'");
    }

    @RestController
    static class PingController {
        @GetMapping("/ping")
        String ping() {
            return "ok";
        }
    }

    @EnableWebSecurity
    static class HeaderSecurityConfig {
        @Bean
        SecurityFilterChain headerFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .headers(headers -> headers
                            .contentTypeOptions(Customizer.withDefaults())
                            .frameOptions(frame -> frame.deny())
                            .httpStrictTransportSecurity(hsts -> hsts
                                    .includeSubDomains(true)
                                    .maxAgeInSeconds(SecurityHeaders.HSTS_MAX_AGE_SECONDS)
                            )
                            .referrerPolicy(referrer -> referrer.policy(
                                    ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                            ))
                            .contentSecurityPolicy(csp -> csp.policyDirectives(
                                    SecurityHeaders.CONTENT_SECURITY_POLICY
                            ))
                            .permissionsPolicyHeader(permissions -> permissions.policy(
                                    SecurityHeaders.PERMISSIONS_POLICY
                            ))
                    );
            return http.build();
        }
    }
}
