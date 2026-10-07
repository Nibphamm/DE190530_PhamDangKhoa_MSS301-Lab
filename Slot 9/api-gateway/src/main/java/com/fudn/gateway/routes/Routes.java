package com.fudn.gateway.routes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration(proxyBeanMethods = false)
public class Routes {
    @Bean
    public RouterFunction<ServerResponse> productServiceRoute(
            @Value("${services.product.url}") String url) {
        return route("product_service")
                .route(RequestPredicates.path("/api/products/**"), http())
                .before(uri(url)).build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderServiceRoute(
            @Value("${services.order.url}") String url) {
        return route("order_service")
                .route(RequestPredicates.path("/api/order/**"), http())
                .before(uri(url)).build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceRoute(
            @Value("${services.inventory.url}") String url) {
        return route("inventory_service")
                .route(RequestPredicates.path("/api/inventory/**"), http())
                .before(uri(url)).build();
    }

    @Bean
    public RouterFunction<ServerResponse> productServiceSwaggerRoute(
            @Value("${services.product.url}") String url) {
        return route("product_service_swagger")
                .route(RequestPredicates.path("/aggregate/product-service/v3/api-docs"), http())
                .before(uri(url))
                .before(setPath("/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> orderServiceSwaggerRoute(
            @Value("${services.order.url}") String url) {
        return route("order_service_swagger")
                .route(RequestPredicates.path("/aggregate/order-service/v3/api-docs"), http())
                .before(uri(url))
                .before(setPath("/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceSwaggerRoute(
            @Value("${services.inventory.url}") String url) {
        return route("inventory_service_swagger")
                .route(RequestPredicates.path("/aggregate/inventory-service/v3/api-docs"), http())
                .before(uri(url))
                .before(setPath("/api-docs"))
                .build();
    }
}
