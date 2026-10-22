package com.mss301.order.client.api;

import com.mss301.order.dto.ReserveRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.*;

@HttpExchange("/api/inventory")
public interface InventoryApi {
    @PostExchange("/reserve")
    void reserve(@RequestBody ReserveRequest request);
    @PostExchange("/release/{orderId}")
    void release(@PathVariable Long orderId);
}
