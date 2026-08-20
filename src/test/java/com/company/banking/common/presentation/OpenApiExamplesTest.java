package com.company.banking.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.banking.account.presentation.AccountController;
import com.company.banking.audit.presentation.AuditController;
import com.company.banking.auth.presentation.AuthController;
import com.company.banking.customer.presentation.CustomerController;
import com.company.banking.dashboard.presentation.DashboardController;
import com.company.banking.transaction.presentation.AccountStatementController;
import com.company.banking.transaction.presentation.TransactionController;
import com.company.banking.transaction.presentation.TransactionHistoryController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class OpenApiExamplesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<Class<?>> CONTROLLERS = List.of(
            HealthController.class,
            AuthController.class,
            CustomerController.class,
            AccountController.class,
            TransactionController.class,
            TransactionHistoryController.class,
            AccountStatementController.class,
            DashboardController.class,
            AuditController.class
    );

    @Test
    void jsonExampleConstantsAreValidObjectsOrArrays() throws Exception {
        for (Field field : OpenApiExamples.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            String name = field.getName();
            if (name.endsWith("_ID") || name.endsWith("_TOKEN")) {
                continue;
            }
            String value = (String) field.get(null);
            JsonNode node = MAPPER.readTree(value);
            assertThat(node.isObject() || node.isArray())
                    .as("%s should parse as a JSON object or array", name)
                    .isTrue();
        }
    }

    @Test
    void everyMappedEndpointHasASuccessOrNoContentExample() {
        List<String> missing = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isMappedEndpoint(method)) {
                    continue;
                }
                if (!hasDocumentedSuccessExample(method)) {
                    missing.add(controller.getSimpleName() + "#" + method.getName());
                }
            }
        }
        assertThat(missing)
                .as("2xx success responses need @ExampleObject unless the status is 204")
                .isEmpty();
    }

    private static boolean isMappedEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(RequestMapping.class);
    }

    private static boolean hasDocumentedSuccessExample(Method method) {
        List<ApiResponse> responses = new ArrayList<>();
        ApiResponses grouped = method.getAnnotation(ApiResponses.class);
        if (grouped != null) {
            responses.addAll(Arrays.asList(grouped.value()));
        }
        responses.addAll(Arrays.asList(method.getAnnotationsByType(ApiResponse.class)));
        boolean sawSuccess = false;
        for (ApiResponse response : responses) {
            if (!isSuccess(response.responseCode())) {
                continue;
            }
            sawSuccess = true;
            if ("204".equals(response.responseCode())) {
                return true;
            }
            if (hasExample(response.content())) {
                return true;
            }
        }
        return !sawSuccess;
    }

    private static boolean isSuccess(String code) {
        return code != null && code.startsWith("2");
    }

    private static boolean hasExample(Content[] contents) {
        if (contents == null) {
            return false;
        }
        for (Content content : contents) {
            ExampleObject[] examples = content.examples();
            if (examples == null) {
                continue;
            }
            for (ExampleObject example : examples) {
                if (example.value() != null && !example.value().isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }
}
