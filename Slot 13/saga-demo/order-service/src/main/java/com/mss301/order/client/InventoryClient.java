package com.mss301.order.client;

import com.mss301.order.dto.ReserveRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Optional;
import com.mss301.order.client.api.InventoryApi;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {

    private final RestClient restClient;
    private final InventoryApi api;

    public InventoryClient(@Qualifier("inventoryRestClient") RestClient restClient, Optional<InventoryApi> api) {
        this.restClient = restClient;
        this.api = api.orElse(null);
    }

    public void reserve(Long orderId, String productId, int quantity) {
        if (api != null) { api.reserve(new ReserveRequest(orderId, productId, quantity)); return; }
        restClient.post()
                .uri("/api/inventory/reserve")
                .body(new ReserveRequest(orderId, productId, quantity))
                .retrieve()          // 4xx/5xx -> ném RestClientResponseException
                .toBodilessEntity();
    }

    public void release(Long orderId) {
        if (api != null) { api.release(orderId); return; }
        restClient.post()
                .uri("/api/inventory/release/{orderId}", orderId)
                .retrieve()
                .toBodilessEntity();
    }
}
