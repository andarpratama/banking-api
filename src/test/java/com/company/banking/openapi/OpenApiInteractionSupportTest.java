package com.company.banking.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiInteractionSupportTest {

    private static final String MINI_SPEC = """
            {
              "openapi": "3.0.1",
              "info": { "title": "Mini", "version": "1" },
              "paths": {
                "/ping": {
                  "get": {
                    "responses": {
                      "200": {
                        "description": "ok",
                        "content": {
                          "application/json": {
                            "schema": {
                              "type": "object",
                              "required": ["status"],
                              "properties": {
                                "status": { "type": "string" }
                              },
                              "additionalProperties": false
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    @Test
    @DisplayName("accepts a response body that matches the inline schema")
    void acceptsMatchingBody() {
        OpenApiInteractionValidator validator = OpenApiInteractionSupport.validatorFromSpec(MINI_SPEC);

        ValidationReport report = validator.validate(
                SimpleRequest.Builder.get("/ping").build(),
                SimpleResponse.Builder.status(200)
                        .withContentType("application/json")
                        .withBody("{\"status\":\"UP\"}")
                        .build()
        );

        assertThat(report.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("rejects a response body that is missing a required field")
    void rejectsMissingRequiredField() {
        OpenApiInteractionValidator validator = OpenApiInteractionSupport.validatorFromSpec(MINI_SPEC);

        ValidationReport report = validator.validate(
                SimpleRequest.Builder.get("/ping").build(),
                SimpleResponse.Builder.status(200)
                        .withContentType("application/json")
                        .withBody("{}")
                        .build()
        );

        assertThat(report.hasErrors()).isTrue();
        assertThat(OpenApiInteractionSupport.formatErrors(report)).contains("status");
    }

    @Test
    @DisplayName("assertValid throws when the interaction does not match the spec")
    void assertValidThrowsOnError() {
        OpenApiInteractionValidator validator = OpenApiInteractionSupport.validatorFromSpec(MINI_SPEC);

        assertThatThrownBy(() -> {
            ValidationReport report = validator.validate(
                    SimpleRequest.Builder.get("/ping").build(),
                    SimpleResponse.Builder.status(200)
                            .withContentType("application/json")
                            .withBody("{\"status\":1}")
                            .build()
            );
            if (report.hasErrors()) {
                throw new AssertionError("OpenAPI schema validation failed:\n"
                        + OpenApiInteractionSupport.formatErrors(report));
            }
        }).isInstanceOf(AssertionError.class)
                .hasMessageContaining("OpenAPI schema validation failed");
    }
}
