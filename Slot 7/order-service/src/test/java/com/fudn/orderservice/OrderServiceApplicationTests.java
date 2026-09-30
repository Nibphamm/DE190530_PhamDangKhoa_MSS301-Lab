package com.fudn.orderservice;

import io.restassured.RestAssured;
import com.fudn.orderservice.stub.InventoryStubs;
import org.springframework.beans.factory.annotation.Autowired;
import com.fudn.orderservice.repository.OrderRepository;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(@ConfigureWireMock(baseUrlProperties = "inventory.url"))
class OrderServiceApplicationTests {

    @ServiceConnection
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.3.0");

    @Autowired
    private OrderRepository orderRepository;

    @LocalServerPort
    private Integer port;

    static {
        mySQLContainer.start();
    }

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        reset();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void shouldSubmitOrder() {
        InventoryStubs.stubInventoryCall("iphone_15", 1);
        String submitOrderJson = """
                {
                     "skuCode": "iphone_15",
                     "price": 1000,
                     "quantity": 1
                }
                """;

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
        verify(1, getRequestedFor(urlEqualTo("/api/inventory?skuCode=iphone_15&quantity=1")));
        assertEquals(1, orderRepository.count());
        var order = orderRepository.findAll().getFirst();
        assertEquals("iphone_15", order.getSkuCode());
        assertEquals(1, order.getQuantity());
        java.util.UUID.fromString(order.getOrderNumber());
    }

    @Test
    void shouldFailOrderWhenProductIsNotInStock() {
        InventoryStubs.stubInventoryOutOfStock("iphone_15", 1000);
        RestAssured.given()
                .contentType("application/json")
                .body("""
                        {"skuCode":"iphone_15","price":1000,"quantity":1000}
                        """)
                .when().post("/api/order")
                .then().statusCode(500);
        verify(1, getRequestedFor(urlEqualTo("/api/inventory?skuCode=iphone_15&quantity=1000")));
        assertEquals(0, orderRepository.count());
    }
}
