package com.company.banking.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers Postgres + Redis for full-stack integration tests.
 * Containers start once per JVM (static) so multiple IT classes can reuse them.
 * Does not activate the H2 {@code test} profile — Flyway runs against real Postgres.
 */
public abstract class AbstractPostgresRedisIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("banking_it")
            .withUsername("banking")
            .withPassword("banking");

    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());

        registry.add("app.jwt.secret", () -> "it-test-jwt-secret-key-min-32-chars!!");
    }
}
