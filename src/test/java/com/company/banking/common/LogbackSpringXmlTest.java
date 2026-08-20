package com.company.banking.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class LogbackSpringXmlTest {

    @Test
    void logbackSpringXmlIsWellFormedAndDeclaresLogstashUdpShip() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/logback-spring.xml")) {
            assertThat(in).isNotNull();
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }

        String source;
        try (InputStream in = getClass().getResourceAsStream("/logback-spring.xml")) {
            assertThat(in).isNotNull();
            source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(source)
                .contains("net.logstash.logback.appender.LogstashUdpSocketAppender")
                .contains("net.logstash.logback.layout.LogstashLayout")
                .contains("ASYNC_LOGSTASH")
                .contains("app.logging.logstash.host");
    }
}
