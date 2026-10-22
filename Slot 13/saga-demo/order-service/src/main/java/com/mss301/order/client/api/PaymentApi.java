package com.mss301.order.client.api;

import com.mss301.order.dto.PaymentRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.*;

@HttpExchange("/api/payments")
public interface PaymentApi {
    @PostExchange("/pay")
    void pay(@RequestBody PaymentRequest request);
    @PostExchange("/refund/{orderId}")
    void refund(@PathVariable Long orderId);
}
