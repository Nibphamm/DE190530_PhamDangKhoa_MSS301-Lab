package com.fudn.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = {
        "services.product.url", "services.order.url", "services.inventory.url"}))
class ApiGatewaySecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void resetWireMock() {
        reset();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void requestWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
        verify(0, getRequestedFor(urlEqualTo("/api/products")));
    }

    @Test
    void requestWithValidJwtShouldBeRoutedToProductService() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/products"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"1\",\"name\":\"iPhone 15\",\"price\":1000}]")));
        mockMvc.perform(get("/api/products").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("iPhone 15")));
        verify(1, getRequestedFor(urlEqualTo("/api/products")));
    }

    @Test
    void postOrderWithValidJwtShouldBeRoutedToOrderService() throws Exception {
        String body = "{\"skuCode\":\"iphone_15\",\"price\":1000,\"quantity\":1}";
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/order"))
                .withRequestBody(equalToJson(body))
                .willReturn(aResponse().withStatus(201).withBody("Order Placed Successfully")));
        mockMvc.perform(post("/api/order").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));
        verify(1, postRequestedFor(urlEqualTo("/api/order"))
                .withRequestBody(equalToJson(body)));
    }

    @Test
    void inventoryRouteShouldForwardQueryParams() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo("iphone_15"))
                .withQueryParam("quantity", equalTo("1"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("true")));
        mockMvc.perform(get("/api/inventory").with(jwt())
                        .param("skuCode", "iphone_15").param("quantity", "1"))
                .andExpect(status().isOk()).andExpect(content().string("true"));
        verify(1, getRequestedFor(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo("iphone_15"))
                .withQueryParam("quantity", equalTo("1")));
    }
}
