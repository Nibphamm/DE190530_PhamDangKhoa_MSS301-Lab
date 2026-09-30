package com.fudn.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "inventory", url = "${inventory.url}")
public interface InventoryClient {
    @GetMapping("/api/inventory")
    boolean isInStock(@RequestParam("skuCode") String skuCode,
                      @RequestParam("quantity") Integer quantity);
}
