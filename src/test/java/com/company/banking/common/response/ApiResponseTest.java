package com.company.banking.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void okWrapsDataWithSuccessFlag() {
        ApiResponse<String> response = ApiResponse.ok("pong");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("pong");
        assertThat(response.message()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void okWithMessageIncludesMessage() {
        ApiResponse<Integer> response = ApiResponse.ok(1, "created");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(1);
        assertThat(response.message()).isEqualTo("created");
    }
}
