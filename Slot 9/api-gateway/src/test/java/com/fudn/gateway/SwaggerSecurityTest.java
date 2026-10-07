package com.fudn.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
        "services.product.url", "services.order.url", "services.inventory.url"}))
class SwaggerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void resetBackend() {
        reset();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void swaggerUiShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void swaggerConfigShouldListAllServices() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls.length()").value(3))
                .andExpect(jsonPath("$.urls[*].name", hasItems(
                        "Product Service", "Order Service", "Inventory Service")))
                .andExpect(jsonPath("$.urls[*].url", hasItems(
                        "/aggregate/product-service/v3/api-docs",
                        "/aggregate/order-service/v3/api-docs",
                        "/aggregate/inventory-service/v3/api-docs")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"product", "order", "inventory"})
    void aggregateDocsShouldBePermittedAndRewritePath(String service) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api-docs"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Backend API\"}}")));
        mockMvc.perform(get("/aggregate/" + service + "-service/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Backend API"));
        verify(1, getRequestedFor(urlEqualTo("/api-docs")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/products", "/api/product", "/api/order", "/api/inventory"})
    void protectedApiShouldRequireToken(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        verify(0, getRequestedFor(anyUrl()));
    }

    @Test
    void corsPreflightShouldAllowAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST"));
    }
}
