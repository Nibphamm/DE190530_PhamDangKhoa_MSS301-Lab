package com.mss301.order.outbox;

import com.mss301.order.client.*;
import com.mss301.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Profile("outbox")
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcher {
    private final OrderOutboxService outbox;
    private final OrderService orders;
    private final InventoryClient inventory;
    private final PaymentClient payment;

    @Scheduled(fixedDelayString = "${saga.outbox.poll-ms:500}")
    public void dispatch() {
        var next = outbox.claim();
        if (next.isEmpty()) return;
        var command = next.get();
        var order = orders.findById(command.getOrderId());
        try {
            switch (command.getCommand()) {
                case RESERVE -> inventory.reserve(order.getId(), order.getProductId(), order.getQuantity());
                case PAY -> payment.pay(order.getId(), order.getCustomerId(), order.getAmount());
                case REFUND -> payment.refund(order.getId());
                case RELEASE -> inventory.release(order.getId());
                case CONFIRM, CANCEL -> { } // Local updates occur atomically with command completion.
            }
        } catch (org.springframework.web.client.RestClientException ex) {
            outbox.failed(command.getId(), ex.getMessage());
            return;
        }
        // If this commit fails, the lease expires and the idempotent command is delivered again.
        outbox.complete(command.getId());
    }
}
