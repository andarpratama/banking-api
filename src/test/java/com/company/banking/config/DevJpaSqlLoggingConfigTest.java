package com.company.banking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards dev-only Hibernate SQL logging (quick-win: query optimization).
 */
class DevJpaSqlLoggingConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void applicationDevYmlEnablesFormattedSqlLogging() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application-dev.yml")) {
            assertThat(in).as("application-dev.yml on classpath").isNotNull();

            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> jpa = (Map<String, Object>) spring.get("jpa");
            Map<String, Object> properties = (Map<String, Object>) jpa.get("properties");
            Map<String, Object> hibernate = (Map<String, Object>) properties.get("hibernate");
            Map<String, Object> logging = (Map<String, Object>) root.get("logging");
            Map<String, Object> level = (Map<String, Object>) logging.get("level");

            assertThat(jpa.get("show-sql")).isEqualTo(true);
            assertThat(hibernate.get("format_sql")).isEqualTo(true);
            assertThat(hibernate.get("use_sql_comments")).isEqualTo(true);
            assertThat(level.get("org.hibernate.SQL")).isEqualTo("DEBUG");
            assertThat(level.get("org.hibernate.orm.jdbc.bind")).isEqualTo("TRACE");
        }
    }
}
