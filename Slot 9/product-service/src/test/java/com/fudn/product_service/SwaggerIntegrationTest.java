package com.fudn.product_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SwaggerIntegrationTest {
    @Container
    static MongoDBContainer database = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", database::getReplicaSetUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerUiShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void apiDocsShouldReturnJson() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("Product Service API"))
                .andExpect(jsonPath("$.info.version").value("v0.0.1"))
                .andExpect(jsonPath("$.info.license.name").value("Apache 2.0"));
    }

    @Test
    void apiDocsShouldContainProductEndpoints() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/products'].get").exists())
                .andExpect(jsonPath("$.paths['/api/products'].post").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].delete").exists());
    }

    @Test
    void apiShouldAllowCrossOriginPreflight() throws Exception {
        mockMvc.perform(options("/api/products")
                        .header("Origin", "http://localhost:9000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:9000"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
