package com.company.banking.auth.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    @Test
    void sha256_shouldBeDeterministicAndNotEqualRawToken() {
        String raw = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example";

        String first = TokenHasher.sha256(raw);
        String second = TokenHasher.sha256(raw);

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(raw);
        assertThat(first).hasSize(64);
    }
}
