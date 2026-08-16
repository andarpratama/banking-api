package com.company.banking.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Adapts MockMvc results to Atlassian OpenAPI request/response validation.
 */
final class OpenApiInteractionSupport {

    private OpenApiInteractionSupport() {
    }

    static OpenApiInteractionValidator validatorFromSpec(String openApiJson) {
        return OpenApiInteractionValidator.createForInlineApiSpecification(openApiJson).build();
    }

    static void assertValid(OpenApiInteractionValidator validator, MvcResult result) {
        ValidationReport report = validator.validate(toRequest(result), toResponse(result));
        if (report.hasErrors()) {
            throw new AssertionError("OpenAPI schema validation failed:\n" + formatErrors(report));
        }
    }

    static SimpleRequest toRequest(MvcResult result) {
        MockHttpServletRequest servletRequest = result.getRequest();
        Request.Method method = Request.Method.valueOf(servletRequest.getMethod());
        SimpleRequest.Builder builder = new SimpleRequest.Builder(method, servletRequest.getRequestURI());

        servletRequest.getParameterMap().forEach((name, values) -> {
            if (values != null) {
                Arrays.stream(values).forEach(value -> builder.withQueryParam(name, value));
            }
        });

        Collections.list(servletRequest.getHeaderNames()).forEach(name ->
                Collections.list(servletRequest.getHeaders(name)).forEach(value -> builder.withHeader(name, value))
        );

        String contentType = servletRequest.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            builder.withContentType(contentType);
        }

        byte[] content = servletRequest.getContentAsByteArray();
        if (content != null && content.length > 0) {
            Charset charset = charsetOrUtf8(servletRequest.getCharacterEncoding());
            builder.withBody(new String(content, charset));
        }
        return builder.build();
    }

    static SimpleResponse toResponse(MvcResult result) {
        MockHttpServletResponse servletResponse = result.getResponse();
        SimpleResponse.Builder builder = SimpleResponse.Builder.status(servletResponse.getStatus());

        servletResponse.getHeaderNames().forEach(name ->
                servletResponse.getHeaders(name).forEach(value -> builder.withHeader(name, value))
        );

        String contentType = servletResponse.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            builder.withContentType(contentType);
        }

        byte[] content = servletResponse.getContentAsByteArray();
        if (content != null && content.length > 0) {
            Charset charset = charsetOrUtf8(servletResponse.getCharacterEncoding());
            builder.withBody(new String(content, charset));
        }
        return builder.build();
    }

    static String formatErrors(ValidationReport report) {
        return report.getMessages().stream()
                .filter(message -> message.getLevel() == ValidationReport.Level.ERROR)
                .map(message -> message.getKey() + ": " + message.getMessage())
                .collect(Collectors.joining("\n"));
    }

    private static Charset charsetOrUtf8(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }
}
