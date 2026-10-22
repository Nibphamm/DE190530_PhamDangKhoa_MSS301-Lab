package com.mss301.order.client;

import com.mss301.order.dto.PaymentRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Optional;
import com.mss301.order.client.api.PaymentApi;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {

    private final RestClient restClient;
    private final PaymentApi api;

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient, Optional<PaymentApi> api) {
        this.restClient = restClient;
        this.api = api.orElse(null);
    }

    public void pay(Long orderId, String customerId, double amount) {
        if (api != null) { api.pay(new PaymentRequest(orderId, customerId, amount)); return; }
        restClient.post()
                .uri("/api/payments/pay")
                .body(new PaymentRequest(orderId, customerId, amount))
                .retrieve()
                .toBodilessEntity();
    }

    public void refund(Long orderId) {
        if (api != null) { api.refund(orderId); return; }
        restClient.post()
                .uri("/api/payments/refund/{orderId}", orderId)
                .retrieve()
                .toBodilessEntity();
    }
}
