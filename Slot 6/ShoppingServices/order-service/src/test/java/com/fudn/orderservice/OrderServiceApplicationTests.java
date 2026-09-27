package com.fudn.orderservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.fudn.orderservice.repository.OrderRepository;
import com.fudn.orderservice.stub.InventoryStubs;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceApplicationTests {

    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    private static final WireMockServer wireMockServer =
            new WireMockServer(options().dynamicPort());

    @LocalServerPort
    private Integer port;

    @Autowired
    private OrderRepository orderRepository;

    static {
        mySQLContainer.start();
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("wiremock.server.port", wireMockServer::port);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        wireMockServer.resetAll();
    }

    @Test
    void shouldSubmitOrder() {
        long ordersBefore = orderRepository.count();
        String submitOrderJson = """
                {
                     "skuCode": "iphone_15",
                     "price": 1000,
                     "quantity": 1
                }
                """;

        InventoryStubs.stubInventoryCall(wireMockServer, "iphone_15", 1);

        var responseBodyString = RestAssured.given()
                .contentType("application/json")
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .body().asString();

        assertThat(responseBodyString, Matchers.is("Order Placed Successfully"));
        assertThat(orderRepository.count(), Matchers.is(ordersBefore + 1));
    }

    @Test
    void shouldRejectOrderWhenInventoryIsOutOfStock() {
        long ordersBefore = orderRepository.count();
        InventoryStubs.stubInventoryCall(wireMockServer, "iphone_15", 101, false);

        RestAssured.given()
                .contentType("application/json")
                .body("""
                        {
                          "skuCode": "iphone_15",
                          "price": 1000,
                          "quantity": 101
                        }
                        """)
                .when()
                .post("/api/order")
                .then()
                .statusCode(500);

        assertThat(orderRepository.count(), Matchers.is(ordersBefore));
    }
}
