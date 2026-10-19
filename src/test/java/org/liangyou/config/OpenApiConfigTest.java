package org.liangyou.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void basicDataIsVisibleInApiDocumentation() {
        OpenApiConfig config = new OpenApiConfig(new SwaggerGlobalHeaderProperties());
        assertEquals(List.of("/api/v1/basic/**"), config.basicDataGroup().getPathsToMatch());
    }

    @Test
    void openApiAddsConfiguredGlobalHeaderSchemes() {
        SwaggerGlobalHeaderProperties properties = new SwaggerGlobalHeaderProperties();
        properties.setGlobalHeaders(List.of("X-Tenant-Id", "X-Trace-Id"));
        OpenApiConfig config = new OpenApiConfig(properties);

        OpenAPI openAPI = config.openAPI();

        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("BearerAuth"));
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("GlobalHeaderXTenantId"));
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("GlobalHeaderXTraceId"));

        SecurityScheme tenantHeader = openAPI.getComponents().getSecuritySchemes().get("GlobalHeaderXTenantId");
        assertEquals(SecurityScheme.Type.APIKEY, tenantHeader.getType());
        assertEquals(SecurityScheme.In.HEADER, tenantHeader.getIn());
        assertEquals("X-Tenant-Id", tenantHeader.getName());
    }

    @Test
    void openApiSkipsBlankDuplicateAndAuthorizationHeader() {
        SwaggerGlobalHeaderProperties properties = new SwaggerGlobalHeaderProperties();
        properties.setGlobalHeaders(List.of(" ", "Authorization", "x-trace-id", "X-Trace-Id"));
        OpenApiConfig config = new OpenApiConfig(properties);

        OpenAPI openAPI = config.openAPI();

        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertFalse(openAPI.getComponents().getSecuritySchemes().containsKey("GlobalHeaderAuthorization"));
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("GlobalHeaderXTraceId"));
        assertEquals(2, openAPI.getComponents().getSecuritySchemes().size());
    }
}
