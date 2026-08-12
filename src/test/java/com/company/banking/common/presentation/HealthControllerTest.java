package com.company.banking.common.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.banking.common.application.HealthCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthCheckService healthCheckService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        HealthCheckService healthCheckService() {
            return new TestHealthCheckService();
        }
    }

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void livenessReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health/live").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessReturnsUpWhenAllHealthy() throws Exception {
        // Arrange
        TestHealthCheckService testService = (TestHealthCheckService) healthCheckService;
        testService.setDatabaseStatus("UP");
        testService.setRedisStatus("UP");

        // Act & Assert
        mockMvc.perform(get("/api/v1/health/ready").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.cache").value("UP"));
    }

    @Test
    void readinessReturnsDownWhenDatabaseUnhealthy() throws Exception {
        // Arrange
        TestHealthCheckService testService = (TestHealthCheckService) healthCheckService;
        testService.setDatabaseStatus("DOWN");
        testService.setRedisStatus("UP");

        // Act & Assert
        mockMvc.perform(get("/api/v1/health/ready").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("DOWN"))
                .andExpect(jsonPath("$.cache").value("UP"));
    }

    @Test
    void readinessReturnsDownWhenRedisUnhealthy() throws Exception {
        // Arrange
        TestHealthCheckService testService = (TestHealthCheckService) healthCheckService;
        testService.setDatabaseStatus("UP");
        testService.setRedisStatus("DOWN");

        // Act & Assert
        mockMvc.perform(get("/api/v1/health/ready").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.cache").value("DOWN"));
    }

    static class TestHealthCheckService implements HealthCheckService {

        private String databaseStatus = "UP";
        private String redisStatus = "UP";

        void setDatabaseStatus(String status) {
            this.databaseStatus = status;
        }

        void setRedisStatus(String status) {
            this.redisStatus = status;
        }

        @Override
        public String checkDatabaseHealth() {
            return databaseStatus;
        }

        @Override
        public String checkRedisHealth() {
            return redisStatus;
        }
    }
}
