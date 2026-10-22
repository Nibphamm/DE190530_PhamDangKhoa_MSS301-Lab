package com.mss301.order.events;

public record SagaEvent(String type, Long orderId, String customerId, String productId,
                        int quantity, double amount, String reason) {
    public SagaEvent withType(String next, String failure) {
        return new SagaEvent(next, orderId, customerId, productId, quantity, amount, failure);
    }
}
