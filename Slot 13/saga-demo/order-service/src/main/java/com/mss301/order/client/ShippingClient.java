package com.mss301.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ShippingClient {
    private final RestClient client;

    public ShippingClient(RestClient.Builder builder, @Value("${services.shipping.url:http://localhost:8084}") String url) {
        client = builder.baseUrl(url).build();
    }

    public record ShippingRequest(Long orderId, String productId, int quantity) { }

    public void create(Long orderId, String productId, int quantity) {
        client.post().uri("/api/shipping/create").body(new ShippingRequest(orderId, productId, quantity))
                .retrieve().toBodilessEntity();
    }

    public void cancel(Long orderId) {
        client.post().uri("/api/shipping/cancel/{id}", orderId).retrieve().toBodilessEntity();
    }
}
